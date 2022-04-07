package stratx.gui.candlestick;

import org.jfree.data.xy.OHLCDataset;

public interface KeyedOHLCDataset extends OHLCDataset {
    int getID(int series, int item);
}
