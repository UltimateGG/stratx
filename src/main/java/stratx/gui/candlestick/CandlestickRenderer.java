package stratx.gui.candlestick;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.AbstractXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.util.PaintUtils;
import org.jfree.chart.util.PublicCloneable;
import org.jfree.chart.util.SerialUtils;
import org.jfree.data.Range;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import stratx.gui.SignalIndicator;
import stratx.utils.Signal;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CandlestickRenderer extends AbstractXYItemRenderer implements XYItemRenderer, Cloneable, PublicCloneable, Serializable {
    private static final long serialVersionUID = 50390895841817621L;
    public static final int WIDTHMETHOD_AVERAGE = 0;
    public static final int WIDTHMETHOD_SMALLEST = 1;
    public static final int WIDTHMETHOD_INTERVALDATA = 2;
    private int autoWidthMethod;
    private double autoWidthFactor;
    private double autoWidthGap;
    private double candleWidth;
    private double maxCandleWidthInMilliseconds;
    private double maxCandleWidth;
    private transient Paint upPaint;
    private transient Paint downPaint;
    private boolean useOutlinePaint;
    private final List<SignalIndicator> indicators = Collections.synchronizedList(new ArrayList<>());


    public CandlestickRenderer(double candleWidth) {
        this.autoWidthMethod = 0;
        this.autoWidthFactor = 0.6428571428571429D;
        this.autoWidthGap = 0.0D;
        this.maxCandleWidthInMilliseconds = 7.2E7D;
        this.candleWidth = candleWidth;
        this.upPaint = new Color(0x22ab94); // From tradingview
        this.downPaint = new Color(0xf23645);
        this.useOutlinePaint = false;
    }

    public double getCandleWidth() {
        return this.candleWidth;
    }

    public void setCandleWidth(double width) {
        if (width != this.candleWidth) {
            this.candleWidth = width;
            this.fireChangeEvent();
        }
    }

    public double getMaxCandleWidthInMilliseconds() {
        return this.maxCandleWidthInMilliseconds;
    }

    public void setMaxCandleWidthInMilliseconds(double millis) {
        this.maxCandleWidthInMilliseconds = millis;
        this.fireChangeEvent();
    }

    public int getAutoWidthMethod() {
        return this.autoWidthMethod;
    }

    public void setAutoWidthMethod(int autoWidthMethod) {
        if (this.autoWidthMethod != autoWidthMethod) {
            this.autoWidthMethod = autoWidthMethod;
            this.fireChangeEvent();
        }
    }

    public double getAutoWidthFactor() {
        return this.autoWidthFactor;
    }

    public void setAutoWidthFactor(double autoWidthFactor) {
        if (this.autoWidthFactor != autoWidthFactor) {
            this.autoWidthFactor = autoWidthFactor;
            this.fireChangeEvent();
        }
    }

    public double getAutoWidthGap() {
        return this.autoWidthGap;
    }

    public void setAutoWidthGap(double autoWidthGap) {
        if (this.autoWidthGap != autoWidthGap) {
            this.autoWidthGap = autoWidthGap;
            this.fireChangeEvent();
        }
    }

    public Paint getUpPaint() {
        return this.upPaint;
    }

    public void setUpPaint(Paint paint) {
        this.upPaint = paint;
        this.fireChangeEvent();
    }

    public Paint getDownPaint() {
        return this.downPaint;
    }

    public void setDownPaint(Paint paint) {
        this.downPaint = paint;
        this.fireChangeEvent();
    }

    public boolean getUseOutlinePaint() {
        return this.useOutlinePaint;
    }

    public void setUseOutlinePaint(boolean use) {
        if (this.useOutlinePaint != use) {
            this.useOutlinePaint = use;
            this.fireChangeEvent();
        }
    }

    public Range findRangeBounds(XYDataset dataset) {
        return this.findRangeBounds(dataset, true);
    }

    public XYItemRendererState initialise(Graphics2D g2, Rectangle2D dataArea, XYPlot plot, XYDataset dataset, PlotRenderingInfo info) {
        ValueAxis axis = plot.getDomainAxis();
        double x1 = axis.getLowerBound();
        double x2 = x1 + this.maxCandleWidthInMilliseconds;
        RectangleEdge edge = plot.getDomainAxisEdge();
        double xx1 = axis.valueToJava2D(x1, dataArea, edge);
        double xx2 = axis.valueToJava2D(x2, dataArea, edge);
        this.maxCandleWidth = Math.abs(xx2 - xx1);

        return new XYItemRendererState(info);
    }

    public void drawItem(Graphics2D g2, XYItemRendererState state, Rectangle2D dataArea, PlotRenderingInfo info, XYPlot plot, ValueAxis domainAxis, ValueAxis rangeAxis, XYDataset dataset, int series, int item, CrosshairState crosshairState, int pass) {
        if (!(dataset instanceof KeyedOHLCDataset)) return;
        PlotOrientation orientation = plot.getOrientation();
        boolean horiz;
        if (orientation == PlotOrientation.HORIZONTAL) {
            horiz = true;
        } else {
            if (orientation != PlotOrientation.VERTICAL) return;

            horiz = false;
        }

        EntityCollection entities = info != null ? info.getOwner().getEntityCollection() : null;
        KeyedOHLCDataset highLowData = (KeyedOHLCDataset)dataset;
        double x = highLowData.getXValue(series, item);
        double yHigh = highLowData.getHighValue(series, item);
        double yLow = highLowData.getLowValue(series, item);
        double yOpen = highLowData.getOpenValue(series, item);
        double yClose = highLowData.getCloseValue(series, item);
        RectangleEdge domainEdge = plot.getDomainAxisEdge();
        double xx = domainAxis.valueToJava2D(x, dataArea, domainEdge);
        RectangleEdge edge = plot.getRangeAxisEdge();
        double yyHigh = rangeAxis.valueToJava2D(yHigh, dataArea, edge);
        double yyLow = rangeAxis.valueToJava2D(yLow, dataArea, edge);
        double yyOpen = rangeAxis.valueToJava2D(yOpen, dataArea, edge);
        double yyClose = rangeAxis.valueToJava2D(yClose, dataArea, edge);
        double stickWidth;
        double yyMaxOpenClose;
        double min;
        double max;

        if (this.candleWidth > 0.0D) {
            stickWidth = this.candleWidth;
        } else {
            double xxWidth = 0.0D;
            int itemCount;

            switch (this.autoWidthMethod) {
                case 0:
                    itemCount = highLowData.getItemCount(series);
                    xxWidth = (horiz ? dataArea.getHeight() : dataArea.getWidth()) / (double)itemCount;
                    break;
                case 1:
                    itemCount = highLowData.getItemCount(series);
                    yyMaxOpenClose = -1.0D;
                    xxWidth = dataArea.getWidth();
                    int i = 0;

                    while (i < itemCount) {
                        min = domainAxis.valueToJava2D(highLowData.getXValue(series, i), dataArea, domainEdge);
                        if (yyMaxOpenClose != -1.0D)
                            xxWidth = Math.min(xxWidth, Math.abs(min - yyMaxOpenClose));

                        yyMaxOpenClose = min;
                        ++i;
                    }
                case 2:
                    IntervalXYDataset intervalXYData = (IntervalXYDataset)dataset;
                    min = domainAxis.valueToJava2D(intervalXYData.getStartXValue(series, item), dataArea, plot.getDomainAxisEdge());
                    max = domainAxis.valueToJava2D(intervalXYData.getEndXValue(series, item), dataArea, plot.getDomainAxisEdge());
                    xxWidth = Math.abs(max - min);
            }

            xxWidth -= 2.0D * this.autoWidthGap;
            xxWidth *= this.autoWidthFactor;
            xxWidth = Math.min(xxWidth, this.maxCandleWidth);
            stickWidth = Math.max(Math.min(3.0D, this.maxCandleWidth), xxWidth);
        }

        Paint p = this.getItemPaint(series, item);
        Paint outlinePaint = null;
        if (this.useOutlinePaint)
            outlinePaint = this.getItemOutlinePaint(series, item);

        Stroke s = this.getItemStroke(series, item);
        g2.setStroke(s);

        // StratX: Overrode so the outline/wick is the candle body color
        Paint p1 = yClose > yOpen ? this.upPaint : this.downPaint;
        p = p1 != null ? p1 : p;

        g2.setPaint(this.useOutlinePaint ? outlinePaint : p);

        yyMaxOpenClose = Math.max(yyOpen, yyClose);
        double yyMinOpenClose = Math.min(yyOpen, yyClose);
        double maxOpenClose = Math.max(yOpen, yClose);
        double minOpenClose = Math.min(yOpen, yClose);
        if (yHigh > maxOpenClose) {
            if (horiz) {
                g2.draw(new java.awt.geom.Line2D.Double(yyHigh, xx, yyMaxOpenClose, xx));
            } else {
                g2.draw(new java.awt.geom.Line2D.Double(xx, yyHigh, xx, yyMaxOpenClose));
            }
        }

        if (yLow < minOpenClose) {
            if (horiz) {
                g2.draw(new java.awt.geom.Line2D.Double(yyLow, xx, yyMinOpenClose, xx));
            } else {
                g2.draw(new java.awt.geom.Line2D.Double(xx, yyLow, xx, yyMinOpenClose));
            }
        }

        double length = Math.abs(yyHigh - yyLow);
        double base = Math.min(yyHigh, yyLow);
        Double body;
        Double hotspot;
        if (horiz) {
            body = new Double(yyMinOpenClose, xx - stickWidth / 2.0D, yyMaxOpenClose - yyMinOpenClose, stickWidth);
            hotspot = new Double(base, xx - stickWidth / 2.0D, length, stickWidth);
        } else {
            body = new Double(xx - stickWidth / 2.0D, yyMinOpenClose, stickWidth, yyMaxOpenClose - yyMinOpenClose);
            hotspot = new Double(xx - stickWidth / 2.0D, base, stickWidth, length);
        }

        if (yClose > yOpen) {
            g2.setPaint(this.upPaint != null ? this.upPaint : p);
            g2.fill(body);
        } else {
            g2.setPaint(this.downPaint != null ? this.downPaint : p);
            g2.fill(body);
        }

        g2.setPaint(this.useOutlinePaint ? outlinePaint : p);
        g2.draw(body);
        if (entities != null)
            this.addEntity(entities, hotspot, dataset, series, item, 0.0D, 0.0D);

        // Indicators display
        synchronized (this.indicators) {
            for (SignalIndicator signal : this.indicators) {
                if (signal.getCandleID() == highLowData.getID(series, item)) {
                    g2.setColor(signal.getType() == Signal.BUY ? Color.GREEN : Color.RED);
                    //g2.setStroke(new BasicStroke(1.0F));
                    g2.drawLine((int) xx, 0, (int) xx, 900);
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof CandlestickRenderer)) {
            return false;
        } else {
            CandlestickRenderer that = (CandlestickRenderer)obj;
            if (this.candleWidth != that.candleWidth) {
                return false;
            } else if (!PaintUtils.equal(this.upPaint, that.upPaint)) {
                return false;
            } else if (!PaintUtils.equal(this.downPaint, that.downPaint)) {
                return false;
            } else if (this.maxCandleWidthInMilliseconds != that.maxCandleWidthInMilliseconds) {
                return false;
            } else if (this.autoWidthMethod != that.autoWidthMethod) {
                return false;
            } else if (this.autoWidthFactor != that.autoWidthFactor) {
                return false;
            } else if (this.autoWidthGap != that.autoWidthGap) {
                return false;
            } else if (this.useOutlinePaint != that.useOutlinePaint) {
                return false;
            } else {
                return super.equals(obj);
            }
        }
    }

    public void addSignalIndicatorOn(int candleID, Signal type) {
        synchronized (this.indicators) {
            this.indicators.add(new SignalIndicator(candleID, type));
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.defaultWriteObject();
        SerialUtils.writePaint(this.upPaint, stream);
        SerialUtils.writePaint(this.downPaint, stream);
    }

    private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
        stream.defaultReadObject();
        this.upPaint = SerialUtils.readPaint(stream);
        this.downPaint = SerialUtils.readPaint(stream);
    }
}
