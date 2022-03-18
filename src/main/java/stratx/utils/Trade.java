package stratx.utils;

import stratx.BackTest;

public class Trade {
    private final BackTest simulation;
    private boolean isOpen;
    private final Candlestick entry;
    private final double entryAmount;
    private final double entryAmountUSD;
    private boolean trailingStopArmed = false;
    private double lastProfitPercent = 0;
    private Candlestick exit;

    public Trade(BackTest simulation, Candlestick entry, double usd) {
        if (usd <= 0) throw new IllegalArgumentException("USD must be positive to enter a trade");
        this.simulation = simulation;
        this.entry = entry;
        this.entryAmountUSD = usd;
        this.entryAmount = usd / entry.getClose();
        this.isOpen = true;

        if (simulation.getGUI() != null)
            simulation.getGUI().getChartRenderer().addSignalIndicatorOn(entry.getID(), Signal.BUY);
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
        this.exit = exit;
        this.isOpen = false;
        if (simulation.getGUI() != null)
            simulation.getGUI().getChartRenderer().addSignalIndicatorOn(exit.getID(), Signal.SELL);
        System.out.println(this + (reason != null ? " (" + reason + ")" : ""));
    }

    /** Returns the current profit in USD */
    public double getProfit() { // If open use the current price
        double priceToUse = isOpen ? simulation.getCurrentCandle().getClose() : exit.getClose();
        double worthInUSDAtEntry = entry.getClose() * entryAmount;
        double worthInUSDNow = priceToUse * entryAmount;
        return worthInUSDNow - worthInUSDAtEntry;
    }

    public double getProfitPercent() {
        return (getProfit() / entryAmountUSD) * 100.0D;
    }

    @Override
    public String toString() {
        return (getProfit() >= 0 ? Utils.ANSI_GREEN + "+" : Utils.ANSI_RED + "-")
                + " $" + Math.abs(MathUtils.roundTwoDec(getProfit())) + " USD "
                + MathUtils.formatPercent(getProfitPercent()) + Utils.ANSI_RESET;
    }
}
