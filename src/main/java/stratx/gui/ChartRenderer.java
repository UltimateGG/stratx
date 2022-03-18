package stratx.gui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.ohlc.OHLCSeries;
import org.jfree.data.time.ohlc.OHLCSeriesCollection;
import stratx.StratX;
import stratx.utils.Candlestick;
import stratx.utils.MathUtils;
import stratx.utils.Signal;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class ChartRenderer extends JPanel {
    private static final DateFormat READABLE_TIME_FORMAT = new SimpleDateFormat("MMM dd HH:mm");
    private final Font labelFont = new Font("Arial", Font.BOLD, 16);
    private final Font tickFont = new Font("Arial", Font.BOLD, 13);
    public static final Color darkThemeColor = new Color(0x171A21);
    public static final Color darkThemeLighterColor = new Color(0x222272F);
    private final JFreeChart candlestickChart;
    private KeyedOHLCSeries ohlcSeries;
    private CandlestickRenderer candlestickRenderer;

    public ChartRenderer(String title, int width, int height) {
        candlestickChart = createChart(title);
        ChartPanel chartPanel = new ChartPanel(candlestickChart);

        chartPanel.setPreferredSize(new Dimension(width, height));
        chartPanel.setMouseZoomable(false);
        chartPanel.setMouseWheelEnabled(false);
        chartPanel.setAutoscrolls(true);
        chartPanel.setPopupMenu(null);

        add(chartPanel, BorderLayout.CENTER);
    }

    private JFreeChart createChart(String chartTitle) {
        // Create OHLCSeriesCollection as a price dataset for candlestick chart
        KeyedOHLCSeriesCollection candlestickDataset = new KeyedOHLCSeriesCollection();
        ohlcSeries = new KeyedOHLCSeries("Price");
        candlestickDataset.addSeries(ohlcSeries);

        // Price axis & label
        NumberAxis priceAxis = new NumberAxis("Price");
        priceAxis.setLabelFont(labelFont);
        priceAxis.setTickLabelFont(tickFont);
        priceAxis.setAutoRangeIncludesZero(false);
        priceAxis.setNumberFormatOverride(MathUtils.COMMAS);
        priceAxis.setLabelPaint(Color.WHITE);
        priceAxis.setTickLabelPaint(Color.WHITE);
        priceAxis.setAxisLinePaint(darkThemeLighterColor);

        // Date axis & label
        DateAxis dateAxis = new DateAxis("Date");
        dateAxis.setLabelFont(labelFont);
        dateAxis.setTickLabelFont(tickFont);
        dateAxis.setDateFormatOverride(READABLE_TIME_FORMAT);
        dateAxis.setLabelPaint(Color.WHITE);
        dateAxis.setTickLabelPaint(Color.WHITE);
        dateAxis.setAxisLinePaint(darkThemeLighterColor);
        dateAxis.setLowerMargin(0.02);
        dateAxis.setUpperMargin(0.02);

        // Create candlestick chart renderer
        candlestickRenderer = new CandlestickRenderer(CandlestickRenderer.WIDTHMETHOD_AVERAGE,
                false, new CustomTooltip(new SimpleDateFormat("kk:mm:ss a"), MathUtils.TWO_DEC));
        candlestickRenderer.setUseOutlinePaint(false);

        // Create subplot
        XYPlot candlestickSubplot = new XYPlot(candlestickDataset, null, priceAxis, candlestickRenderer);
        candlestickSubplot.setBackgroundPaint(darkThemeColor);
        candlestickSubplot.setRangePannable(false);
        candlestickSubplot.setDomainPannable(false);
        candlestickSubplot.setDomainGridlinePaint(darkThemeLighterColor);
        candlestickSubplot.setRangeGridlinePaint(darkThemeLighterColor);
        candlestickSubplot.setDomainGridlineStroke(new BasicStroke(1.0F));
        candlestickSubplot.setRangeGridlineStroke(new BasicStroke(1.0F));
        candlestickSubplot.setOutlinePaint(darkThemeLighterColor);

        // Create mainPlot
        CombinedDomainXYPlot mainPlot = new CombinedDomainXYPlot(dateAxis);
        mainPlot.setGap(100.0);
        mainPlot.add(candlestickSubplot, 3);
        mainPlot.setOrientation(PlotOrientation.VERTICAL);
        mainPlot.setOutlinePaint(darkThemeLighterColor);

        JFreeChart chart = new JFreeChart(chartTitle, new Font("Arial", Font.BOLD, 16), mainPlot, false);
        chart.getTitle().setPaint(Color.WHITE);
        chart.setBackgroundPaint(darkThemeColor);

        return chart;
    }

    public void populate(List<Candlestick> data, boolean autoScale, int maxCandles) {
        int size = data.size();
        int scale = autoScale ? Math.max(1, size / maxCandles) : 1;
        Candlestick previous = null;

        if (scale > 1) {
            StratX.log("Scaling chart by %d", scale);
            this.setTitle(this.getTitle() + " (Scaled x" + scale + ")");
        }

        // Populate the chart
        for (int i = 0; i < size; i += scale) {
            if (i == 0) { // First candle, has no previous
                this.addCandle(data.get(i));
                continue;
            }

            if (scale == 1) {
                this.addCandle(data.get(i).toHeikinAshi(data.get(i - 1)));
                continue;
            }

            // Collapse the data we skipped into a single candle
            double maxOpen = data.get(i).getOpen();
            double maxHigh = data.get(i).getHigh();
            double maxLow = data.get(i).getLow();
            double maxClose = data.get(i).getClose();
            long volume = data.get(i).getVolume();

            for (int j = i - scale; j < i; j++) {
                maxHigh = Math.max(maxHigh, data.get(j).getHigh());
                maxLow = Math.min(maxLow, data.get(j).getLow());
                volume += data.get(j).getVolume();
            }

            Candlestick candle = new Candlestick(
                    data.get(i).getDate(),
                    maxOpen,
                    maxHigh,
                    maxLow,
                    maxClose,
                    volume
            ).toHeikinAshi(previous);
            this.addCandle(candle);
            previous = candle;
        }
    }

    public void addCandle(Candlestick c) {
        ohlcSeries.add(new FixedMillisecond(new Date(Long.parseLong(c.getDate()))),
                c.getOpen(),
                c.getHigh(),
                c.getLow(),
                c.getClose(),
                c.getID()
        );
    }

    public void addSignalIndicatorOn(int candleID, Signal type) {
        candlestickRenderer.addSignalIndicatorOn(candleID, type);
    }

    public void clearData() {
        ohlcSeries.clear();
    }

    public CandlestickRenderer getCandlestickRenderer() {
        return candlestickRenderer;
    }

    public String getTitle() {
        return candlestickChart.getTitle().getText();
    }

    public void setTitle(String title) {
        candlestickChart.setTitle(title);
    }
}
