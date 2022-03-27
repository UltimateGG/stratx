package stratx.gui;

import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.ohlc.OHLCSeries;

public class KeyedOHLCSeries extends OHLCSeries {
    private Class<? extends RegularTimePeriod> timeClass = null;

    public KeyedOHLCSeries(Comparable<?> key) {
        super(key);
    }

    // We only fire notify on last candle if running huge backtest for performance
    public void add(RegularTimePeriod period, double open, double high, double low, double close, int id, boolean isLast) {
        if (timeClass == null) timeClass = period.getClass();
        else if (!timeClass.equals(period.getClass()))
            throw new IllegalArgumentException("Can't mix RegularTimePeriod class types.");

        super.add(new KeyedOHLCItem(period, open, high, low, close, id), isLast);
    }
}
