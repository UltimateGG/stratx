package stratx.utils;

import stratx.BackTest;
import stratx.StratX;

public class Trade {
    private static int MAX_ID = 0;
    private final int ID = MAX_ID++;
    private final BackTest simulation;
    private boolean isOpen;
    private final Candlestick entry;
    private final double entryAmount;
    private final double entryAmountUSD;
    private boolean trailingStopArmed = false;
    private double lastProfitPercent = 0;
    private Candlestick exit;
    private String closeReason;

    public Trade(BackTest simulation, Candlestick entry, double usd) {
        if (usd <= 0.0) throw new IllegalArgumentException("USD must be positive to enter a trade");
        this.simulation = simulation;
        this.entry = entry;
        this.entryAmountUSD = usd;
        this.entryAmount = usd / entry.getClose();
        this.isOpen = true;

        if (simulation.getGUI() != null && simulation.shouldShowSignals())
            simulation.getGUI().getChartRenderer().addSignalIndicatorOn(entry.getID(), Signal.BUY);
        StratX.trace("[BUY] {} {} @ ${}/ea for ${}",
                MathUtils.COMMAS_2F.format(this.entryAmount),
                simulation.getCoin(),
                MathUtils.round(entry.getClose(), 4),
                MathUtils.COMMAS_2F.format(usd));
    }

    public boolean isOpen() {
        return isOpen;
    }

    public Candlestick getEntry() {
        return entry;
    }

    public double getEnterAmount() {
        return entryAmount;
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

    public Candlestick getExit() {
        return exit;
    }

    public void close(Candlestick exit, String reason) {
        if (!isOpen) throw new IllegalStateException("Trade is already closed");
        this.exit = exit;
        this.isOpen = false;
        if (simulation.getGUI() != null && simulation.shouldShowSignals())
            simulation.getGUI().getChartRenderer().addSignalIndicatorOn(exit.getID(), Signal.SELL);
        this.closeReason = reason;
        simulation.getLogger().info(this.toString());
        StratX.trace("[SELL] ({}) {} {} @ ${}/ea for profit of ${} ({}%)",
                reason,
                MathUtils.COMMAS_2F.format(this.entryAmount),
                simulation.getCoin(),
                MathUtils.round(exit.getClose(), 4),
                MathUtils.COMMAS_2F.format(getProfit()),
                MathUtils.COMMAS_2F.format(getProfitPercent()));
    }

    /** Returns the current profit in USD */
    public double getProfit() { // If open use the current price
        double priceToUse = isOpen ? simulation.getCurrentCandle().getClose() : exit.getClose();
        double worthInUSDAtEntry = entry.getClose() * entryAmount;
        double worthInUSDNow = priceToUse * entryAmount;
        return worthInUSDNow - worthInUSDAtEntry;
    }

    /** Returns the current profit % */
    public double getProfitPercent() {
        return (getProfit() / entryAmountUSD) * 100.0D;
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
