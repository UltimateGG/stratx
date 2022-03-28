package stratx.utils;

import stratx.exceptions.InvalidCandlestickException;

/** Immutable class representing a candlestick/OHLC values. */
public class Candlestick {
    private static int MAX_ID = 0;
    private int ID = MAX_ID++;
    private final long date;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;
    private final Candlestick previous;


    public Candlestick(long date, double open, double high, double low, double close, long volume, Candlestick previous) throws InvalidCandlestickException {
        if (date < 0 || open < 0 || high < 0 || low < 0 || close < 0 || volume < 0)
            throw new InvalidCandlestickException("Invalid candlestick values (Must be positive)");

        this.date = date;

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

    public double getOpen() {
        return open;
    }

    public double getHigh() {
        return high;
    }

    public double getLow() {
        return low;
    }

    public double getClose() {
        return close;
    }

    public long getVolume() {
        return volume;
    }

    public long getDate() {
        return date;
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
        return String.format("[Candlestick @ %s] O:%.2f, H:%.2f, L:%.2f, C:%.2f, V:%d ID:%d", date, open, high, low, close, volume, ID);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Candlestick)) return false;

        Candlestick other = (Candlestick) obj;
        return this.ID == other.getID() && this.date == other.date && this.open == other.open && this.high == other.high && this.low == other.low && this.close == other.close && this.volume == other.volume;
    }

    @Override
    public Candlestick clone() throws CloneNotSupportedException {
        super.clone();
        Candlestick clone = new Candlestick(date, open, high, low, close, volume, this.previous);
        clone.setID(ID); // Persist id
        return clone;
    }
}
