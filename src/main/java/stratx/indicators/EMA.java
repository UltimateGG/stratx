package stratx.indicators;

import org.jfree.data.xy.XYSeries;
import stratx.modes.Mode;
import stratx.utils.Candlestick;
import stratx.utils.Configuration;
import stratx.utils.PriceHistory;
import stratx.utils.Signal;

import java.awt.*;

public class EMA extends Indicator {
    private final int period;
    private final PriceHistory priceHistory;
    private XYSeries emaLine;

    private boolean SHOW_ON_CHART = true;
    private Color COLOR = new Color(0x0F74E7);
    private float LINE_WIDTH = 2.0F;


    public EMA(Mode mode, int period) {
        super(mode);
        this.period = period;
        this.priceHistory = new PriceHistory(period);
        this.loadSettings(mode.getConfig());
    }

    @Override
    public void update(Candlestick candle) {
        priceHistory.add(candle);

        if (mode.isShowGUI() && SHOW_ON_CHART && priceHistory.length() >= period) {
            if (emaLine == null) emaLine = mode.getGUI().getCandlestickChart().addEMALine(COLOR, LINE_WIDTH);
            else emaLine.add(candle.getCloseTime(), getEMA(candle));
        }
    }

    @Override
    public Signal getSignal() {
        Candlestick last = priceHistory.getLatest();
        double ema = getEMA(last);
        if (ema == -1) return Signal.HOLD;
        if (ema > last.getClose()) return Signal.SELL;
        if (ema < last.getClose()) return Signal.BUY;
        return Signal.HOLD;
    }

    private double getEMA(Candlestick current) {
        if (priceHistory.length() < period) return -1;

        double k = 2.0 / (period + 1.0D);
        double ema = current.getClose();
        for (Candlestick candle : priceHistory.get())
            ema += k * (candle.getClose() - ema);
        return ema;
    }

    public void loadSettings(Configuration config) {
        SHOW_ON_CHART = config.getBoolean("indicators.ema.show-on-chart", SHOW_ON_CHART);
        COLOR = config.getColor("indicators.ema.color", COLOR);
        LINE_WIDTH = (float) config.getDouble("indicators.ema.line-width", LINE_WIDTH);
    }
}
