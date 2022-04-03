package stratx.utils;

import stratx.BackTest;
import stratx.StratX;

public class Trade {
    private static int MAX_ID = 0;
    private final int ID = MAX_ID++;
    private final BackTest simulation;
    private boolean isOpen;
    private final Candlestick entryCandle;
    private final long entryTime;
    private final double coinAmount;
    private final double entryAmountUSD; // initial USD amount purchased
    private boolean trailingStopArmed = false;
    private double lastProfitPercent = 0;
    private Candlestick exitCandle;
    private long exitTime;
    private String closeReason;

    public Trade(BackTest simulation, double usd) {
        if (usd <= 0.0) throw new IllegalArgumentException("USD must be positive to enter a trade");
        this.simulation = simulation;
        this.entryCandle = simulation.getCurrentCandle();
        this.entryTime = entryCandle.getDate();
        this.entryAmountUSD = usd;
        this.coinAmount = usd / simulation.getCurrentPrice();
        this.isOpen = true;

        if (simulation.shouldShowSignals())
            simulation.getGUI().getChartRenderer().addSignalIndicatorOn(entryCandle.getID(), Signal.BUY);

        StratX.trace("[BUY] {} {} @ ${}/ea for ${}",
                MathUtils.COMMAS_2F.format(this.coinAmount),
                simulation.getCoin(),
                MathUtils.round(entryCandle.getClose(), 4),
                MathUtils.COMMAS_2F.format(usd));
    }

    public boolean isOpen() {
        return isOpen;
    }

    public Candlestick getEntryCandle() {
        return entryCandle;
    }

    public double getCoinAmount() {
        return coinAmount;
    }

    public double getEntryAmountUSD() {
        return entryAmountUSD;
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

    public Candlestick getExitCandle() {
        return exitCandle;
    }

    public void close(String reason) {
        if (!isOpen) throw new IllegalStateException("Trade is already closed");
        this.isOpen = false;
        this.exitCandle = simulation.getCurrentCandle();
        this.exitTime = exitCandle.getDate();
        this.closeReason = reason;

        if (simulation.shouldShowSignals())
            simulation.getGUI().getChartRenderer().addSignalIndicatorOn(exitCandle.getID(), Signal.SELL);

        simulation.getLogger().info(this.toString());
        StratX.trace("[SELL] ({}) {} {} @ ${}/ea for profit of ${} ({}%)",
                reason,
                MathUtils.COMMAS_2F.format(this.coinAmount),
                simulation.getCoin(),
                MathUtils.round(exitCandle.getClose(), 4),
                MathUtils.COMMAS_2F.format(getProfit()),
                MathUtils.COMMAS_2F.format(getProfitPercent()));
    }

    /** Returns the current profit in USD */
    public double getProfit() {
        return getCurrentUSDWorth() - entryAmountUSD;
    }

    /** Returns the current profit % */
    public double getProfitPercent() {
        return (getProfit() / entryAmountUSD) * 100.0D;
    }

    /** Returns the current value of this trade in USD */
    public double getCurrentUSDWorth() {
        double priceToUse = isOpen ? simulation.getCurrentPrice() : exitCandle.getClose();
        return priceToUse * coinAmount;
    }

    public long getHoldingTime() {
        return isOpen ? System.currentTimeMillis() - entryTime : exitTime - entryTime;
    }

    @Override
    public String toString() {
        return (isOpen ? "[OPEN] " : "")
                + (getProfit() >= 0 ? Utils.ANSI_GREEN + "+" : Utils.ANSI_RED + "-")
                + " $" + Math.abs(MathUtils.roundTwoDec(getProfit())) + " USD "
                + MathUtils.formatPercent(getProfitPercent()) + Utils.ANSI_RESET
                + (" ($" + MathUtils.COMMAS_2F.format(entryAmountUSD) + ")")
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
