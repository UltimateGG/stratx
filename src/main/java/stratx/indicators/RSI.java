package stratx.indicators;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import stratx.BackTest;
import stratx.gui.ChartRenderer;
import stratx.utils.Candlestick;
import stratx.utils.MathUtils;
import stratx.utils.PriceHistory;
import stratx.utils.Signal;

import java.awt.*;

import static stratx.gui.ChartRenderer.styleAxis;
import static stratx.gui.ChartRenderer.stylePlot;

public class RSI extends Indicator implements IIndicator {
    private final int period;
    private final double overbought;
    private final double oversold;
    private XYPlot rsiSubplot;
    private XYSeries overboughtLine;
    private XYSeries oversoldLine;
    private XYSeries midLine;
    private XYSeries rsiLine;

    private final PriceHistory priceHistory;

    // @TODO make this configurable
    private final boolean SHOW_ON_CHART = true;
    private final boolean SHOW_MID_LINE = true;
    private final Color COLOR = new Color(0x7E44F1);
    private final Color OVERBOUGHT_COLOR = new Color(0xF33232);
    private final Color OVERSOLD_COLOR = new Color(0x36F54F);
    private final Color MIDDLE_COLOR = new Color(0x6C6C6C);
    private final float LINE_WIDTH = 1.5F;

    public RSI(BackTest simulation, int period, double overbought, double oversold) {
        super(simulation);
        this.period = period;
        this.overbought = overbought;
        this.oversold = oversold;
        this.priceHistory = new PriceHistory(period);
    }

    @Override
    public void update(Candlestick candle) {
        priceHistory.add(candle);

        if (SHOW_ON_CHART && priceHistory.length() >= period) {
            if (rsiLine == null && simulation.isShowGUI()) { // Create RSI overlay
                ChartRenderer renderer = simulation.getGUI().getChartRenderer();
                XYLineAndShapeRenderer emaRenderer = new XYLineAndShapeRenderer(true, false);
                emaRenderer.setSeriesPaint(0, OVERBOUGHT_COLOR);
                emaRenderer.setSeriesStroke(0, new BasicStroke(1.0F));
                emaRenderer.setSeriesPaint(1, OVERSOLD_COLOR);
                emaRenderer.setSeriesStroke(1, new BasicStroke(1.0F));
                emaRenderer.setSeriesPaint(2, COLOR);
                emaRenderer.setSeriesStroke(2, new BasicStroke(LINE_WIDTH));

                XYSeriesCollection rsiDataset = new XYSeriesCollection();
                XYSeries rsiSeries = new XYSeries("RSI");
                rsiDataset.addSeries(rsiSeries);

                NumberAxis rsiAxis = new NumberAxis("RSI");
                styleAxis(rsiAxis);

                rsiSubplot = new XYPlot(rsiDataset, renderer.getDateAxis(), rsiAxis, emaRenderer);
                stylePlot(rsiSubplot);

                overboughtLine = addLine(OVERBOUGHT_COLOR, 1.0F);
                oversoldLine = addLine(OVERSOLD_COLOR, 1.0F);
                midLine = addLine(MIDDLE_COLOR, 1.0F);
                rsiLine = addLine(COLOR, LINE_WIDTH);
                renderer.getMainPlot().add(rsiSubplot, 2);
            } else if (overboughtLine != null) {
                long x = Long.parseLong(candle.getDate());
                overboughtLine.add(x, overbought);
                oversoldLine.add(x, oversold);
                if (SHOW_MID_LINE) midLine.add(x, (overbought + oversold) / 2);
                rsiLine.add(x, getRSI());
            }
        }
    }

    private XYSeries addLine(Color color, float width) {
        // Create ema line overlay
        int num = rsiSubplot.getDatasetCount() + 1;
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("RSI");
        dataset.addSeries(series);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesStroke(0, new BasicStroke(width));

        rsiSubplot.setDataset(num, dataset);
        rsiSubplot.setRenderer(num, renderer);
        return series;
    }

    @Override
    public Signal getSignal() {
        double rsi = getRSI();
        if (rsi == -1) return Signal.HOLD;
        if (rsi > overbought) return Signal.SELL;
        if (rsi < oversold) return Signal.BUY;
        return Signal.HOLD;
    }

    private double getRSI() {
        if (priceHistory.length() < period) return -1; // not enough data yet
        double up = 0;
        double down = 0;

        for (int i = 0; i < period; i++) {
            double change = priceHistory.get(i).getClose() - priceHistory.get(i).getOpen();
            if (change > 0) up += change;
            else down -= change;
        }

        double rs = (up / period) / (down / period);
        return MathUtils.clampDouble(100 - (100 / (1 + rs)), 0.0D, 100.0D);
    }
}
