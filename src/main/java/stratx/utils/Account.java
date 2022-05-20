package stratx.utils;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import stratx.StratX;
import stratx.modes.Mode;

import java.math.BigDecimal;
import java.util.ArrayList;

public class Account {
    private final double initialBalance;
    private double balance;
    private final ArrayList<Trade> trades = new ArrayList<>();
    private int openTrades = 0;
    private final double BUY_SELL_FEE;

    public Account(double balance, double buySellFee) {
        this.balance = balance;
        this.initialBalance = balance;
        this.BUY_SELL_FEE = buySellFee;
    }

    public double getBalance() {
        if (StratX.MODE == Mode.Type.LIVE)
            return Double.parseDouble(StratX.API.get().getAccount().getAssetBalance(StratX.getTradingAsset()).getFree());

        return balance;
    }

    public void openTrade(Trade trade) {
        if (check()) return;
        trades.add(trade);

        if (StratX.MODE == Mode.Type.LIVE) {
            placeOrder(trade, OrderSide.BUY);
            return;
        }

        openTrades++;
        balance -= trade.getAmountUSD();
        StratX.trace("New Balance: ${} ({} open trades)\n", MathUtils.COMMAS_2F.format(balance), openTrades);
    }

    public void closeTrade(Trade trade, String reason) {
        if (check()) return;

        if (StratX.MODE == Mode.Type.LIVE) {
            if (placeOrder(trade, OrderSide.SELL))
                trade.close(reason);

            return;
        }

        trade.close(reason);
        openTrades--;
        balance += trade.getCurrentUSDWorth();
        StratX.trace("New Balance: ${} ({} open trades)\n", MathUtils.COMMAS_2F.format(balance), openTrades);
    }

    private boolean check() {
        if (StratX.MODE.requiresMarketDataStream() && !StratX.getCurrentMode().isConnectedToMarket()) {
            StratX.warn("Cannot open trade while not connected to market! (Internet down)");
            return true;
        }

        return false;
    }

    private boolean placeOrder(Trade trade, OrderSide side) {
        try {
            if (trade.getOrder() == null && side == OrderSide.SELL) {
                StratX.warn("No trade placed for {}, cannot sell.", trade);
                return false;
            } else if (trade.getOrder() != null && side == OrderSide.BUY) {
                StratX.warn("Trade already placed for {}, cannot buy.", trade);
                return false;
            }

            double amount = trade.getAmount();
            if (side == OrderSide.SELL) {
                amount = 0;
                for (com.binance.api.client.domain.account.Trade fill : trade.getOrder().getFills())
                    amount += Double.parseDouble(fill.getQty());
                StratX.log("sell for {}", amount);
            }
            String convertedAmount = Utils.convertTradeAmount(amount, StratX.getCurrentMode().getCoin());
            String limit = BigDecimal.valueOf(side == OrderSide.BUY ? trade.getEntryPrice() : StratX.getCurrentMode().getCurrentPrice()).toString();
            NewOrderResponse res = StratX.API.get().newOrder(
                    side == OrderSide.BUY ? NewOrder.limitBuy(StratX.getCurrentMode().getCoin(), TimeInForce.IOC, convertedAmount, limit)
                            .newOrderRespType(NewOrderResponseType.FULL)
                            : NewOrder.limitSell(StratX.getCurrentMode().getCoin(), TimeInForce.IOC, convertedAmount, limit)
                            .newOrderRespType(NewOrderResponseType.FULL)
            );

            if (side == OrderSide.BUY) trade.serOrder(res);
            StratX.both("Placed limit {} order for {} {} (${} USD) Execute at {}", side, MathUtils.roundTwoDec(trade.getAmount()), StratX.getCurrentMode().getCoin(), trade.getAmountUSD(), limit);

            if (res.getStatus() == OrderStatus.FILLED) StratX.both("Order filled!");
            else StratX.both("Order not yet filled! ({})", res.getStatus());

            if (res.getStatus() == OrderStatus.REJECTED) {
                StratX.both("Order rejected, closing trade!", res.getStatus());
                trade.close("Order rejected");
                return false;
            }

            double fillsQty = 0;
            double fillsPrice = 0;

            for (com.binance.api.client.domain.account.Trade fill : res.getFills()) {
                double qty = Double.parseDouble(fill.getQty());
                fillsQty += qty - Double.parseDouble(fill.getCommission());
                fillsPrice += qty * Double.parseDouble(fill.getPrice());
            }

            if (side == OrderSide.BUY) {
                balance -= fillsPrice;
                openTrades++;
            } else {
                balance += fillsPrice;
                openTrades--;
            }

            StratX.trace("[{}] Fills: {} {} @ ${}\n", side, fillsQty, StratX.getCurrentMode().getCoin(), fillsPrice / fillsQty);
        } catch (Exception e) {
            StratX.getCurrentMode().getLogger().error("Error placing order", e);
            trade.close("Failed to place order");
            return false;
        }

        return true;
    }

    public ArrayList<Trade> getTrades() {
        return trades;
    }

    public int getOpenTrades() {
        return openTrades;
    }

    public double getBuySellFee() {
        return BUY_SELL_FEE;
    }

    public void reset() {
        balance = initialBalance;
        openTrades = 0;
        trades.clear();
    }
}
