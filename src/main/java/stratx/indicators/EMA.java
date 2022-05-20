package stratx.indicators;

import org.jfree.data.xy.XYSeries;
import stratx.StratX;
import stratx.utils.Candlestick;
import stratx.utils.Configuration;
import stratx.utils.PriceHistory;
import stratx.utils.Signal;

import java.awt.*;

public class EMA extends Indicator {
    private final int period;
    private XYSeries emaLine;

    private boolean SHOW_ON_CHART = true;
    private Color COLOR = new Color(0x0F74E7);
    private float LINE_WIDTH = 2.0F;


    public EMA(int period) {
        super("EMA");
        this.period = period;
        this.priceHistory = new PriceHistory(period);
        this.loadSettings(StratX.getConfig());
    }

    @Override
    public void update(Candlestick candle) {
        priceHistory.add(candle);

        if (StratX.getCurrentMode().isShowGUI() && SHOW_ON_CHART && priceHistory.length() >= period) {
            if (emaLine == null) emaLine = StratX.getCurrentMode().getGUI().getCandlestickChart().addEMALine(COLOR, LINE_WIDTH);
            else emaLine.add(candle.getCloseTime(), getEMA(candle.getClose()));
        }
    }

    @Override
    public Signal getSignal() {
        double current = StratX.getCurrentMode().getCurrentPrice();
        double ema = getEMA(current);

        if (ema == -1) return Signal.HOLD;
        if (current > ema) return Signal.BUY;
        if (current < ema) return Signal.SELL;
        return Signal.HOLD;
    }

    private double getEMA(double currentPrice) {
        if (priceHistory.length() < period) return -1;

        double k = 2.0 / (period + 1.0D);
        double ema = currentPrice;
        double prevEMA = priceHistory.get(period - 1).getClose();
        for (Candlestick candle : priceHistory.get()) {
            ema = k * (candle.getClose() - prevEMA) + prevEMA;
            prevEMA = ema;
        }

        return ema;
    }

    public void loadSettings(Configuration config) {
        SHOW_ON_CHART = config.getBoolean("indicators.ema.show-on-chart", SHOW_ON_CHART);
        COLOR = config.getColor("indicators.ema.color", COLOR);
        LINE_WIDTH = (float) config.getDouble("indicators.ema.line-width", LINE_WIDTH);
    }
}
