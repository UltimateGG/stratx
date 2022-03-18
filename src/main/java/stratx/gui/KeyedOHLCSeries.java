package stratx.gui;

import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.ohlc.OHLCItem;
import org.jfree.data.time.ohlc.OHLCSeries;

public class KeyedOHLCSeries extends OHLCSeries {
    public KeyedOHLCSeries(Comparable key) {
        super(key);
    }

    public void add(RegularTimePeriod period, double open, double high, double low, double close, int id) {
        if (getItemCount() > 0) {
            OHLCItem item0 = (OHLCItem) this.getDataItem(0);
            if (!period.getClass().equals(item0.getPeriod().getClass())) {
                throw new IllegalArgumentException(
                        "Can't mix RegularTimePeriod class types.");
            }
        }
        super.add(new KeyedOHLCItem(period, open, high, low, close, id), true);
    }
}
