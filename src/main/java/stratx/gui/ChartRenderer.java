package stratx.gui;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import stratx.utils.Candlestick;
import stratx.utils.MathUtils;
import stratx.utils.Signal;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class ChartRenderer extends JPanel {
    private static final DateFormat READABLE_TIME_FORMAT = new SimpleDateFormat("MMM dd HH:mm");
    public static final Font labelFont = new Font("Arial", Font.BOLD, 16);
    public static final Font tickFont = new Font("Arial", Font.BOLD, 13);
    public static final Color darkThemeColor = new Color(0x171A21);
    public static final Color darkThemeLighterColor = new Color(0x222272F);
    private final JFreeChart candlestickChart;
    private final ChartPanel chartPanel;
    private KeyedOHLCSeries ohlcSeries;
    private CombinedDomainXYPlot mainPlot;
    private DateAxis dateAxis;
    private NumberAxis priceAxis;
    private XYPlot candlestickSubplot;
    private CandlestickRenderer candlestickRenderer;

    public ChartRenderer(String title, int width, int height) {
        candlestickChart = createChart(title);
        chartPanel = new ChartPanel(candlestickChart);

        chartPanel.setPreferredSize(new Dimension(width, height));
        chartPanel.setMouseZoomable(true);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setAutoscrolls(true);
        chartPanel.setPopupMenu(null);
        mainPlot.setDomainPannable(true);
        mainPlot.setRangePannable(true);

        // Disable stupid hold ctrl to drag
        try {
            Field panMask = chartPanel.getClass().getDeclaredField("panMask");
            panMask.setAccessible(true);
            panMask.set(chartPanel, 0);
        } catch (Exception ignored) {}

        add(chartPanel, BorderLayout.CENTER);
    }

    private JFreeChart createChart(String chartTitle) {
        // Create OHLCSeriesCollection as a price dataset for candlestick chart
        KeyedOHLCSeriesCollection candlestickDataset = new KeyedOHLCSeriesCollection();
        ohlcSeries = new KeyedOHLCSeries("Price");
        candlestickDataset.addSeries(ohlcSeries);

        // Price axis & label
        priceAxis = new NumberAxis("Price");
        styleAxis(priceAxis);

        // Date axis & label
        dateAxis = new DateAxis("Date");
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

        // Create candlestick overlay
        candlestickSubplot = new XYPlot(candlestickDataset, null, priceAxis, candlestickRenderer);
        candlestickSubplot.setBackgroundPaint(darkThemeColor);
        stylePlot(candlestickSubplot);

        // Create mainPlot
        mainPlot = new CombinedDomainXYPlot(dateAxis);
        mainPlot.setGap(1.0);
        mainPlot.add(candlestickSubplot, 10);
        mainPlot.setOrientation(PlotOrientation.VERTICAL);
        mainPlot.setOutlinePaint(darkThemeLighterColor);

        JFreeChart chart = new JFreeChart(chartTitle, new Font("Arial", Font.BOLD, 16), mainPlot, false);
        chart.getTitle().setPaint(Color.WHITE);
        chart.setBackgroundPaint(darkThemeColor);

        return chart;
    }

    public static void styleAxis(NumberAxis priceAxis) {
        priceAxis.setLabelFont(labelFont);
        priceAxis.setTickLabelFont(tickFont);
        priceAxis.setAutoRangeIncludesZero(false);
        priceAxis.setNumberFormatOverride(MathUtils.COMMAS);
        priceAxis.setLabelPaint(Color.WHITE);
        priceAxis.setTickLabelPaint(Color.WHITE);
        priceAxis.setAxisLinePaint(darkThemeLighterColor);
    }

    public static void stylePlot(XYPlot plot) {
        plot.setRangePannable(false);
        plot.setDomainPannable(false);
        plot.setDomainGridlinePaint(darkThemeLighterColor);
        plot.setRangeGridlinePaint(darkThemeLighterColor);
        plot.setBackgroundPaint(ChartRenderer.darkThemeColor);
        plot.setDomainGridlineStroke(new BasicStroke(1.0F));
        plot.setRangeGridlineStroke(new BasicStroke(1.0F));
        plot.setOutlinePaint(darkThemeLighterColor);
    }

    public XYSeries addEMALine(Color color, float width) {
        // Create ema line overlay
        int num = candlestickSubplot.getDatasetCount() + 1;
        XYSeriesCollection emaDataset = new XYSeriesCollection();
        XYSeries emaSeries = new XYSeries("EMA " + num);
        emaDataset.addSeries(emaSeries);

        XYLineAndShapeRenderer emaRenderer = new XYLineAndShapeRenderer(true, false);
        emaRenderer.setSeriesPaint(0, color);
        emaRenderer.setSeriesStroke(0, new BasicStroke(width));

        candlestickSubplot.setDataset(num, emaDataset);
        candlestickSubplot.setRenderer(num, emaRenderer);
        return emaSeries;
    }

    public CombinedDomainXYPlot getMainPlot() {
        return mainPlot;
    }

    public DateAxis getDateAxis() {
        return dateAxis;
    }

    public NumberAxis getPriceAxis() {
        return priceAxis;
    }

    private void addCandle(Candlestick candle) {
        this.addCandle(candle, true);
    }

    public void addCandle(Candlestick c, boolean isLast) {
        ohlcSeries.add(new FixedMillisecond(c.getDate()),
                c.getOpen(),
                c.getHigh(),
                c.getLow(),
                c.getClose(),
                c.getID(),
                isLast
        );
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
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
