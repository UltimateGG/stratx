package stratx.utils;

/** A class representing a candlestick/OHLC values. */
public class Candlestick {
    private static int MAX_ID = 0;
    private int ID = MAX_ID++;
    private final long closeTime;
    private boolean isFinal;
    private final double open;
    private double high;
    private double low;
    private double close;
    private long volume;
    private final Candlestick previous;


    public Candlestick(long closeTime, double open, double high, double low, double close, long volume, Candlestick previous, boolean isFinal) throws RuntimeException {
        this(closeTime, open, high, low, close, volume, previous);
        this.isFinal = isFinal;
    }

    public Candlestick(long closeTime, double open, double high, double low, double close, long volume, Candlestick previous) throws RuntimeException {
        if (closeTime < 0 || open < 0 || high < 0 || low < 0 || close < 0 || volume < 0)
            throw new RuntimeException("Invalid candlestick values (Must be positive)");

        this.closeTime = closeTime;
        this.isFinal = true;

        if (previous == null) {
            this.open = open;
            this.high = high;
            this.low = low;
            this.close = close;
        } else {
            this.open = (previous.getOpen() + previous.getClose()) / 2;
            this.high = Math.max(high, Math.max(open, close));
            this.low = Math.min(low, Math.min(open, close));
            this.close = (open + high + low + close) / 4;
        }

        this.volume = volume;
        this.previous = previous;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
    }

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public void setHigh(double high) {
        if (isFinal) throw new IllegalStateException("Cannot modify closed candlestick");
        this.high = high;
    }

    public double getLow() {
        return low;
    }

    public void setLow(double low) {
        if (isFinal) throw new IllegalStateException("Cannot modify closed candlestick");
        this.low = low;
    }

    public double getClose() {
        return close;
    }

    public void setClose(double close) {
        if (isFinal) throw new IllegalStateException("Cannot modify closed candlestick");
        this.close = close;
    }

    public long getVolume() {
        return volume;
    }

    public void setVolume(long volume) {
        if (isFinal) throw new IllegalStateException("Cannot modify closed candlestick");
        this.volume = volume;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public int getID() {
        return ID;
    }

    protected void setID(int ID) {
        this.ID = ID;
    }

    public double getChange() {
        return close - open;
    }

    public double getChangePercent() {
        return (close - open) / open;
    }

    @Override
    public String toString() {
        return String.format("[Candlestick @ %s] O:%.2f, H:%.2f, L:%.2f, C:%.2f, V:%d ID:%d", closeTime, open, high, low, close, volume, ID);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Candlestick)) return false;

        Candlestick other = (Candlestick) obj;
        return this.ID == other.getID() && this.closeTime == other.closeTime && this.open == other.open && this.high == other.high && this.low == other.low && this.close == other.close && this.volume == other.volume;
    }

    @Override
    public Candlestick clone() throws CloneNotSupportedException {
        super.clone();
        Candlestick clone = new Candlestick(closeTime, open, high, low, close, volume, this.previous);
        clone.setID(ID); // Persist id
        return clone;
    }
}
