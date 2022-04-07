package stratx.gui.candlestick;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import stratx.gui.GuiTheme;
import stratx.utils.Candlestick;
import stratx.utils.MathUtils;
import stratx.utils.Signal;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class CandlestickChart extends JPanel {
    private static final DateFormat READABLE_TIME_FORMAT = new SimpleDateFormat("MMM dd HH:mm");
    private final ChartPanel chartPanel;
    private KeyedOHLCSeries ohlcSeries;
    private CombinedDomainXYPlot mainPlot;
    private DateAxis dateAxis;
    private XYPlot candlestickSubplot;
    private CandlestickRenderer candlestickRenderer;
    private final ArrayList<XYSeries> EVENT_LOCK = new ArrayList<>();

    public CandlestickChart(String title, int width, int height) {
        JFreeChart candlestickChart = createChart(title);
        chartPanel = new ChartPanel(candlestickChart);

        chartPanel.setPreferredSize(new Dimension(width, height));
        chartPanel.setMouseZoomable(true);
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setAutoscrolls(true);
        chartPanel.setPopupMenu(null);
        mainPlot.setDomainPannable(true);
        mainPlot.setRangePannable(true);

        try { // Disable holding ctrl to drag
            Field panMask = chartPanel.getClass().getDeclaredField("panMask");
            panMask.setAccessible(true);
            panMask.set(chartPanel, 0);
        } catch (Exception ignored) {}

        this.setBackground(GuiTheme.PRIMARY_COLOR);
        this.add(chartPanel, BorderLayout.CENTER);
    }

    private JFreeChart createChart(String chartTitle) {
        // Create OHLCSeriesCollection as a price dataset for candlestick chart
        KeyedOHLCSeriesCollection candlestickDataset = new KeyedOHLCSeriesCollection();
        ohlcSeries = new KeyedOHLCSeries("Price");
        ohlcSeries.setNotify(false);
        candlestickDataset.addSeries(ohlcSeries);

        // Price axis & label
        NumberAxis priceAxis = new NumberAxis("Price");
        styleAxis(priceAxis);
        priceAxis.setAutoRangeIncludesZero(false);
        priceAxis.setNumberFormatOverride(MathUtils.COMMAS);

        // Date axis & label
        dateAxis = new DateAxis("Date");
        styleAxis(dateAxis);
        dateAxis.setDateFormatOverride(READABLE_TIME_FORMAT);
        dateAxis.setLowerMargin(0.02);
        dateAxis.setUpperMargin(0.02);

        // Create candlestick chart renderer
        candlestickRenderer = new CandlestickRenderer(CandlestickRenderer.WIDTHMETHOD_AVERAGE);
        candlestickRenderer.setUseOutlinePaint(false);

        // Create candlestick overlay
        candlestickSubplot = new XYPlot(candlestickDataset, null, priceAxis, candlestickRenderer);
        candlestickSubplot.setBackgroundPaint(GuiTheme.PRIMARY_COLOR);
        stylePlot(candlestickSubplot);

        // Create mainPlot
        mainPlot = new CombinedDomainXYPlot(dateAxis);
        mainPlot.setGap(1.0);
        mainPlot.add(candlestickSubplot, 10);
        mainPlot.setOrientation(PlotOrientation.VERTICAL);
        mainPlot.setOutlinePaint(GuiTheme.SECONDARY_COLOR);

        JFreeChart chart = new JFreeChart(chartTitle, new Font("Arial", Font.BOLD, 16), mainPlot, false);
        chart.getTitle().setPaint(Color.WHITE);
        chart.setBackgroundPaint(GuiTheme.PRIMARY_COLOR);

        return chart;
    }

    public static void styleAxis(ValueAxis axis) {
        axis.setLabelFont(GuiTheme.FONT_MD);
        axis.setTickLabelFont(GuiTheme.FONT_SM);
        axis.setLabelPaint(Color.WHITE);
        axis.setTickLabelPaint(Color.WHITE);
        axis.setAxisLinePaint(GuiTheme.SECONDARY_COLOR);
    }

    public static void stylePlot(XYPlot plot) {
        plot.setRangePannable(false);
        plot.setDomainPannable(false);
        plot.setDomainGridlinePaint(GuiTheme.SECONDARY_COLOR);
        plot.setRangeGridlinePaint(GuiTheme.SECONDARY_COLOR);
        plot.setBackgroundPaint(GuiTheme.PRIMARY_COLOR);
        plot.setDomainGridlineStroke(new BasicStroke(1.0F));
        plot.setRangeGridlineStroke(new BasicStroke(1.0F));
        plot.setOutlinePaint(GuiTheme.SECONDARY_COLOR);
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
        this.addPlot(candlestickSubplot);
        return emaSeries;
    }

    public DateAxis getDateAxis() {
        return dateAxis;
    }

    public void addCandle(Candlestick c) {
        ohlcSeries.add(new FixedMillisecond(c.getCloseTime()),
                c.getOpen(),
                c.getHigh(),
                c.getLow(),
                c.getClose(),
                c.getID()
        );
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    public void addSignalIndicatorOn(int candleID, Signal type) {
        candlestickRenderer.addSignalIndicatorOn(candleID, type);
    }

    public void clearCandles() {
        ohlcSeries.clear();
        this.update();
    }

    public void addPlot(XYPlot plot) {
        mainPlot.add(plot, 2);

        for (int i = 0; i < plot.getDatasetCount(); i++) {
            XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset(i);

            for (int j = 0; j < dataset.getSeriesCount(); j++) {
                XYSeries series = dataset.getSeries(j);
                series.setNotify(false);
                EVENT_LOCK.add(series);
            }
        }
    }

    public void update() {
        for (XYSeries series : EVENT_LOCK) {
            series.setNotify(true);
            series.setNotify(false);
        }

        ohlcSeries.setNotify(true);
        ohlcSeries.setNotify(false);
    }
}
