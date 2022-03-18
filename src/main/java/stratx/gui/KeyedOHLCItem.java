package stratx.gui;

import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.ohlc.OHLCItem;

public class KeyedOHLCItem extends OHLCItem {
    private final int id;

    public KeyedOHLCItem(RegularTimePeriod period, double open, double high, double low, double close, int id) {
        super(period, open, high, low, close);
        this.id = id;
    }

    public int getID() {
        return id;
    }
}
