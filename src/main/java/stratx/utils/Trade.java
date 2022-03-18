package stratx.utils;

public class Trade {
    private boolean isOpen;
    private final Candlestick entry;
    private final double entryAmount;
    private final double entryAmountUSD;
    private boolean trailingStopArmed = false;
    private double lastProfitPercent = 0;
    private Candlestick exit;

    public Trade(Candlestick entry, double usd) {
        if (usd <= 0) throw new IllegalArgumentException("USD must be positive to enter a trade");
        this.entry = entry;
        this.entryAmountUSD = usd;
        this.entryAmount = usd / entry.getClose();
        this.isOpen = true;
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
        // @TODO temp
        System.out.println((getProfit() >= 0 ? "\u001B[32m+" : "\u001B[31m-") + " $" + Math.abs(MathUtils.roundTwoDec(getProfit()))
                + " USD " + MathUtils.formatPercent(getProfitPercent()) + "\u001B[0m" + (reason != null ? " (" + reason + ")" : ""));
    }

    /** Returns the profit in USD */
    public double getProfit() {
        if (isOpen) return 0;
        double worthInUSDAtEntry = entry.getClose() * entryAmount;
        double worthInUSDAtExit = exit.getClose() * entryAmount;
        return worthInUSDAtExit - worthInUSDAtEntry;
    }

    public double getProfitPercent() {
        return (getProfit() / entryAmountUSD) * 100.0D;
    }

    public double getCurrentProfit(double price) {
        double worthInUSDAtEntry = entry.getClose() * entryAmount;
        double worthInUSDAtExit = price * entryAmount;
        return worthInUSDAtExit - worthInUSDAtEntry;
    }

    public double getCurrentProfitPercent(double price) {
        return (getCurrentProfit(price) / entryAmountUSD) * 100.0D;
    }
}
