package stratx.indicators;

import org.jfree.data.xy.XYSeries;
import stratx.StratX;
import stratx.utils.Candlestick;
import stratx.utils.Configuration;
import stratx.utils.PriceHistory;
import stratx.utils.Signal;

import java.awt.*;

public class WMA extends Indicator {
    private final int period;
    private final PriceHistory priceHistory;
    private XYSeries wmaLine;

    private boolean SHOW_ON_CHART = true;
    private Color COLOR = new Color(0xFFBF41);
    private float LINE_WIDTH = 2.0F;


    public WMA(int period) {
        this.period = period;
        this.priceHistory = new PriceHistory(period);
        this.loadSettings();
    }

    @Override
    public void update(Candlestick candle) {
        priceHistory.add(candle);

        if (StratX.getCurrentMode().isShowGUI() && SHOW_ON_CHART && priceHistory.length() >= period) {
            if (wmaLine == null) wmaLine = StratX.getCurrentMode().getGUI().getCandlestickChart().addEMALine(COLOR, LINE_WIDTH);
            else wmaLine.add(candle.getCloseTime(), getWMA());
        }
    }

    @Override
    public Signal getSignal() {
        Candlestick last = priceHistory.getLatest();
        double wma = getWMA();
        if (wma == -1) return Signal.HOLD;
        if (wma > last.getClose()) return Signal.SELL;
        if (wma < last.getClose()) return Signal.BUY;
        return Signal.HOLD;
    }

    private double getWMA() {
        if (priceHistory.length() < period) return -1;
        double sum = 0;
        double denom = 0;
        for (int i = 0; i < period; i++) {
            sum += priceHistory.get(i).getClose() * ((i + 1) / (double) period);
            denom += ((i + 1) / (double) period);
        }
        return sum / denom;
    }

    public void loadSettings() {
        Configuration config = StratX.getConfig();
        SHOW_ON_CHART = config.getBoolean("indicators.wma.show-on-chart", SHOW_ON_CHART);
        COLOR = config.getColor("indicators.wma.color", COLOR);
        LINE_WIDTH = (float) config.getDouble("indicators.wma.line-width", LINE_WIDTH);
    }
}
