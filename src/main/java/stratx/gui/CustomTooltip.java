package stratx.gui;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Date;

import org.jfree.chart.labels.HighLowItemLabelGenerator;
import org.jfree.data.xy.OHLCDataset;
import org.jfree.data.xy.XYDataset;

public class CustomTooltip extends HighLowItemLabelGenerator {
    private final DateFormat dateFormatter;
    private final NumberFormat numberFormatter;

    /**
     * Creates a tool tip generator using the supplied date formatter.
     *
     * @param dateFormatter
     *            the date formatter (<code>null</code> not permitted).
     * @param numberFormatter
     *            the number formatter (<code>null</code> not permitted).
     */
    public CustomTooltip(DateFormat dateFormatter, NumberFormat numberFormatter) {
        if (dateFormatter == null)
            throw new IllegalArgumentException("Null 'dateFormatter' argument.");
        if (numberFormatter == null)
            throw new IllegalArgumentException("Null 'numberFormatter' argument.");

        this.dateFormatter = dateFormatter;
        this.numberFormatter = numberFormatter;
    }

    /**
     * Generates a tooltip text item for a particular item within a series.
     *
     * @param dataset
     *            the dataset.
     * @param series
     *            the series (zero-based index).
     * @param item
     *            the item (zero-based index).
     *
     * @return The tooltip text.
     */
    @Override
    public String generateToolTip(XYDataset dataset, int series, int item) {
        if (!(dataset instanceof OHLCDataset)) return null;
        StringBuilder sb = new StringBuilder();

        OHLCDataset d = (OHLCDataset) dataset;
        Number high = d.getHigh(series, item);
        Number low = d.getLow(series, item);
        Number open = d.getOpen(series, item);
        Number close = d.getClose(series, item);
        Number volume = d.getVolume(series, item);
        Number x = d.getX(series, item);

        if (x != null) {
            sb.append("TIME: ").append(this.dateFormatter.format(new Date(x.longValue())));
            if (open != null) sb.append(" | OPEN: ").append(this.numberFormatter.format(open.doubleValue()));
            if (high != null) sb.append(" | HIGH: ").append(this.numberFormatter.format(high.doubleValue()));
            if (low != null) sb.append(" | LOW: ").append(this.numberFormatter.format(low.doubleValue()));
            if (close != null) sb.append(" | CLOSE: ").append(this.numberFormatter.format(close.doubleValue()));
            if (volume != null) sb.append(" | VOL: ").append(this.numberFormatter.format(volume.doubleValue()));
        }

        return sb.toString();
    }

}