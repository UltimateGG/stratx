package stratx.utils;

import stratx.exceptions.InvalidCandlestickException;

/** Immutable class representing a candlestick/OHLC values. */
public class Candlestick {
    private static int MAX_ID = 0;
    private final int ID = MAX_ID++;
    private final String date;
    private final double open;
    private final double high;
    private final double low;
    private final double close;
    private final long volume;


    public Candlestick(String date, double open, double high, double low, double close, long volume) throws InvalidCandlestickException {
        if (date == null || open < 0 || high < 0 || low < 0 || close < 0 || volume < 0)
            throw new InvalidCandlestickException("Invalid candlestick values (Must be positive)");

        this.date = date;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
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

    public String getDate() {
        return date;
    }

    public int getID() {
        return ID;
    }

    public double getChange() {
        return close - open;
    }

    public double getChangePercent() {
        return (close - open) / open;
    }

    public Candlestick toHeikinAshi(Candlestick previous) {
        if (previous == null) return this;
        return new Candlestick(
                date,
                (previous.getOpen() + previous.getClose()) / 2,
                Math.max(high, Math.max(open, close)),
                Math.min(low, Math.min(open, close)),
                (open + high + low + close) / 4,
                volume
        );
    }

    @Override
    public String toString() {
        return String.format("[Candlestick @ %s] O:%.2f, H:%.2f, L:%.2f, C:%.2f, V:%d", date, open, high, low, close, volume);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (!(obj instanceof Candlestick)) return false;

        Candlestick other = (Candlestick) obj;
        return this.ID == other.getID() && this.date.equals(other.date) && this.open == other.open && this.high == other.high && this.low == other.low && this.close == other.close && this.volume == other.volume;
    }

    @Override
    public Candlestick clone() throws CloneNotSupportedException {
        super.clone();
        return new Candlestick(date, open, high, low, close, volume);
    }

    @SuppressWarnings("unused")
    public static enum Interval {
        ONE_MINUTE("1m", 1),
        THREE_MINUTES("3m", 3),
        FIVE_MINUTES("5m", 5),
        FIFTEEN_MINUTES("15m", 15),
        THIRTY_MINUTES("30m", 30),
        ONE_HOUR("1h", 1),
        TWO_HOURS("2h", 2),
        FOUR_HOURS("4h", 4),
        SIX_HOURS("6h", 6),
        EIGHT_HOURS("8h", 8),
        TWELVE_HOURS("12h", 12),
        ONE_DAY("1d", 1),
        THREE_DAYS("3d", 3),
        ONE_WEEK("1w", 1),
        ONE_MONTH("1mo", 1),
        THREE_MONTHS("3mo", 3),
        ONE_YEAR("1y", 1);

        private final String interval;
        private final int value;

        Interval(String interval, int value) {
            this.interval = interval;
            this.value = value;
        }

        public String getInterval() {
            return interval;
        }

        public String toLongName() {
            return toLongName(this);
        }

        public static String toLongName(Candlestick.Interval interval) {
            return toLongName(interval.toString());
        }

        public static String toLongName(String interval) {
            if (interval.endsWith("m")) {
                return "minutes";
            } else if (interval.endsWith("h")) {
                return "hours";
            } else if (interval.endsWith("d")) {
                return "days";
            } else if (interval.endsWith("w")) {
                return "weeks";
            } else if (interval.endsWith("mo")) {
                return "months";
            } else if (interval.endsWith("y")) {
                return "years";
            }

            return null;
        }

        public int getValue() {
            return value;
        }

        @Override
        public String toString() {
            return interval;
        }
    }
}
