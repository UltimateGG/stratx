package stratx.indicators;

import org.jfree.data.xy.XYSeries;
import stratx.BackTest;
import stratx.utils.Candlestick;
import stratx.utils.PriceHistory;
import stratx.utils.Signal;

import java.awt.*;

public class EMA extends Indicator implements IIndicator {
    private final int period;

    // @TODO make this configurable
    private final boolean SHOW_ON_CHART = true;
    private final Color COLOR = new Color(0x0F74E7);
    private final float LINE_WIDTH = 2.0F;

    private final PriceHistory priceHistory;
    private XYSeries emaLine;

    public EMA(BackTest simulation, int period) {
        super(simulation);
        this.period = period;
        this.priceHistory = new PriceHistory(period);
    }

    @Override
    public void update(Candlestick candle) {
        priceHistory.add(candle);

        if (simulation.isShowGUI() && SHOW_ON_CHART && priceHistory.length() >= period) {
            if (emaLine == null) emaLine = simulation.getGUI().getChartRenderer().addEMALine(COLOR, LINE_WIDTH);
            else emaLine.add(candle.getDate(), getEMA(candle));
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
}
