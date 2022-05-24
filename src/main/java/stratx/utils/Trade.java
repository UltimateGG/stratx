package stratx.utils;

import com.binance.api.client.domain.account.NewOrderResponse;
import stratx.StratX;
import stratx.modes.Mode;

public class Trade {
    private static int MAX_ID = 0;
    private final int ID = MAX_ID++;

    private final Mode mode;

    /** If the trade is open or not */
    private boolean isOpen;

    /** The USD price of the trade @ entry */
    private final double entryPrice;

    /** The epoch time of entry */
    private final long entryTime;

    /** The coin amount purchased (Ex 0.1 btc) */
    private final double amount;

    /** The USD worth at entry */
    private final double amountUSD;

    private boolean trailingStopArmed = false;
    private double lastProfitPercent = 0;

    /** The USD price of the trade @ exit */
    private double exitPrice;

    /** The epoch time of exit */
    private long exitTime;

    /** The reason the trade was closed */
    private String closeReason;

    /** The binance trade (If in live mode) */
    private NewOrderResponse order;


    /** Opens trade */
    public Trade(double usd) {
        if (usd <= 0.0) throw new IllegalArgumentException("USD must be positive to enter a trade");
        this.mode = StratX.getCurrentMode();
        this.entryPrice = mode.getCurrentPrice();
        this.entryTime = mode.getCurrentTime();
        usd -= usd * StratX.getCurrentMode().getAccount().getBuySellFee();
        this.amountUSD = usd;
        if (mode.getType() == Mode.Type.SIMULATION) {
            String converted = Utils.convertTradeAmount(usd / entryPrice, StratX.getCurrentMode().getCoin().toString());
            if (converted != null) this.amount = Double.parseDouble(converted);
            else {
                this.amount = amountUSD;
                mode.getLogger().warn("Could not convert trade amount to coin amount");
            }
        } else this.amount = usd / entryPrice;
        this.isOpen = true;

        if (mode.shouldShowSignals())
            mode.getGUI().getCandlestickChart().addSignalIndicatorOn(mode.getCurrentCandle().getID(), Signal.BUY);

        if (mode.getType() != Mode.Type.BACKTEST) mode.getLogger().info("[BUY] " + this);
        StratX.trace("[BUY] {} {} @ ${}/ea for ${}",
                MathUtils.round(this.amount, 8),
                mode.getCoin(),
                MathUtils.round(mode.getCurrentPrice(), 8),
                MathUtils.COMMAS_2F.format(usd));
    }

    public boolean isOpen() {
        return isOpen;
    }

    public double getEntryPrice() {
        return entryPrice;
    }

    public double getAmount() {
        return amount;
    }

    public double getAmountUSD() {
        return amountUSD;
    }

    public boolean isTrailingStopArmed() {
        return trailingStopArmed;
    }

    public void setTrailingStopArmed(boolean armed) {
        this.trailingStopArmed = armed;
    }

    public double getLastProfitPercent() {
        return lastProfitPercent;
    }

    public void setLastProfitPercent(double percent) {
        this.lastProfitPercent = percent;
    }

    public double getExitPrice() {
        return exitPrice;
    }

    public void close(String reason) {
        if (!isOpen) throw new IllegalStateException("Trade is already closed");
        this.isOpen = false;
        this.exitPrice = mode.getCurrentPrice();
        this.exitTime = mode.getCurrentTime();
        this.closeReason = reason;

        if ("Failed to place order".equals(reason)) return;
        if (mode.shouldShowSignals())
            mode.getGUI().getCandlestickChart().addSignalIndicatorOn(mode.getCurrentCandle().getID(), Signal.SELL);

        mode.getLogger().info("[SELL] " + this);
        StratX.trace("[SELL] ({}) {} {} @ ${}/ea for profit of ${} ({}%)",
                reason,
                MathUtils.round(this.amount, 8),
                mode.getCoin(),
                MathUtils.round(mode.getCurrentPrice(), 8),
                MathUtils.COMMAS_2F.format(getProfit()),
                MathUtils.COMMAS_2F.format(getProfitPercent()));
    }

    public NewOrderResponse getOrder() {
        return order;
    }

    public void setOrder(NewOrderResponse res) {
        this.order = res;
    }

    /** Returns the current profit in USD */
    public double getProfit() {
        return (getCurrentUSDWorth() - amountUSD);
    }

    /** Returns the current profit % */
    public double getProfitPercent() {
        return (getProfit() / amountUSD) * 100.0D;
    }

    /** Returns the current value of this trade in USD */
    public double getCurrentUSDWorth() {
        double priceToUse = isOpen ? mode.getCurrentPrice() : exitPrice;
        double currentWorth = priceToUse * amount;
        currentWorth -= currentWorth * StratX.getCurrentMode().getAccount().getBuySellFee();
        return currentWorth;
    }

    public long getHoldingTime() {
        return isOpen ? System.currentTimeMillis() - entryTime : exitTime - entryTime;
    }

    @Override
    public String toString() {
        if (isOpen) {
            return String.format("%s %s ($%s USD) @ $%s/ea",
                    MathUtils.round(amount, 6),
                    mode.getCoin(),
                    MathUtils.round(amountUSD, 2),
                    MathUtils.round(entryPrice, 6));
        }

        return (getProfit() >= 0 ? Utils.ANSI_GREEN + "+" : Utils.ANSI_RED + "-")
                + " $" + Math.abs(MathUtils.roundTwoDec(getProfit())) + " USD "
                + MathUtils.formatPercent(getProfitPercent()) + Utils.ANSI_RESET
                + " ($" + MathUtils.COMMAS_2F.format(amountUSD) + ")"
                + (closeReason != null ? " (" + closeReason + ")" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (o == this) return true;
        if (!(o instanceof Trade)) return false;
        return ((Trade)o).ID == ID;
    }
}
