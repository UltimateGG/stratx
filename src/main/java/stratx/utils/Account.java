package stratx.utils;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderStatus;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;
import com.binance.api.client.domain.account.NewOrderResponse;
import com.binance.api.client.domain.account.NewOrderResponseType;
import com.binance.api.client.domain.account.request.CancelOrderRequest;
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
            return Double.parseDouble(StratX.API.get().getAccount().getAssetBalance(StratX.getCurrentMode().getCoin().getFiat()).getFree());

        return balance;
    }

    public void openTrade(Trade trade) {
        if (check()) return;

        if (StratX.MODE == Mode.Type.LIVE) {
            if (placeOrder(trade, OrderSide.BUY)) trades.add(trade);
            return;
        }

        trades.add(trade);
        openTrades++;
        balance -= trade.getAmountUSD();
        StratX.trace("New Balance: ${} ({} open trades)\n", MathUtils.COMMAS_2F.format(balance), openTrades);
    }

    public void closeTrade(Trade trade, String reason) {
        if (check()) return;

        if (StratX.MODE == Mode.Type.LIVE) {
            if (trade == null) { // Force sell any crypto asset we have, get from binance
                double bal = Double.parseDouble(StratX.API.get().getAccount().getAssetBalance(StratX.getCurrentMode().getCoin().getCrypto()).getFree());
                if (bal > 0.0D) placeOrder(null, OrderSide.SELL);
            } else if (placeOrder(trade, OrderSide.SELL))
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
            if (side == OrderSide.BUY && trade.getOrder() != null) {
                StratX.warn("Trade already placed for {}, cannot buy.", trade);
                return false;
            }

            double amount;
            if (side == OrderSide.SELL) {
                amount = Double.parseDouble(StratX.API.get().getAccount().getAssetBalance(StratX.getCurrentMode().getCoin().getCrypto()).getFree());
                StratX.log("Sell for {}", amount);
            } else amount = trade.getAmount();

            String convertedAmount = Utils.convertTradeAmount(amount, StratX.getCurrentMode().getCoin().toString());
            String limit = BigDecimal.valueOf(side == OrderSide.BUY ? trade.getEntryPrice() : StratX.getCurrentMode().getCurrentPrice()).toString();

            if (convertedAmount == null) return false;

            for (int i = 0; i < StratX.getConfig().getInt("live-trading.max-order-retries", 3); i++) {
                StratX.both("Order attempt #{}", i + 1);

                NewOrderResponse res;
                boolean market = false;
                if (StratX.getConfig().getInt("live-trading.market-order-on-retry", 3) == i + 1) {
                    res = StratX.API.get().newOrder(
                            side == OrderSide.BUY ? NewOrder.marketBuy(StratX.getCurrentMode().getCoin().toString(), convertedAmount)
                                    .newOrderRespType(NewOrderResponseType.FULL)
                                    : NewOrder.marketSell(StratX.getCurrentMode().getCoin().toString(), convertedAmount)
                                    .newOrderRespType(NewOrderResponseType.FULL)
                    );
                    market = true;
                } else {
                    res = StratX.API.get().newOrder(
                            side == OrderSide.BUY ? NewOrder.limitBuy(StratX.getCurrentMode().getCoin().toString(), TimeInForce.IOC, convertedAmount, limit)
                                    .newOrderRespType(NewOrderResponseType.FULL)
                                    : NewOrder.limitSell(StratX.getCurrentMode().getCoin().toString(), TimeInForce.IOC, convertedAmount, limit)
                                    .newOrderRespType(NewOrderResponseType.FULL)
                    );
                }

                double usd = trade == null ? Double.parseDouble(StratX.API.get().getPrice(StratX.getCurrentMode().getCoin().toString()).getPrice()) * amount : trade.getAmountUSD();
                StratX.both("Placed {} {} order for {} {} (${} USD) Execute at {}", market ? "market" : "limit", side, MathUtils.round(amount, 6), StratX.getCurrentMode().getCoin().toString(), usd, limit);

                if (res.getStatus() != OrderStatus.FILLED) {
                    StratX.both("Order not yet filled! ({})", res.getStatus());

                    if (res.getStatus() == OrderStatus.REJECTED) {
                        StratX.both("Order rejected, closing trade!");
                        if (trade != null) trade.close("Order rejected");
                    } else if (res.getStatus() != OrderStatus.CANCELED && res.getStatus() != OrderStatus.PENDING_CANCEL && res.getStatus() != OrderStatus.EXPIRED) { // Cancel order
                        StratX.both("Canceling order {}", res.getOrderId());
                        CancelOrderRequest creq = new CancelOrderRequest(StratX.getCurrentMode().getCoin().toString(), res.getOrderId());
                        StratX.API.get().cancelOrder(creq);
                    }

                    if (res.getStatus() == OrderStatus.EXPIRED) {
                        Thread.sleep(StratX.getConfig().getInt("live-trading.order-retry-delay", 500));
                        continue;
                    } else return false;
                }

                StratX.both("Order filled!");
                if (side == OrderSide.BUY) trade.setOrder(res);

                openTrades += (side == OrderSide.BUY) ? 1 : -1;
                StratX.both("[{}/Success] {} {}\n", side, res.getExecutedQty(), StratX.getCurrentMode().getCoin());
                return true;
            }
        } catch (Exception e) {
            StratX.getCurrentMode().getLogger().error("Error placing order", e);
            StratX.trace("Error: " + e.getMessage());
            if (side == OrderSide.BUY) trade.close("Failed to place order");
        }

        return false;
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
