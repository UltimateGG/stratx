package stratx.indicators;

import org.jfree.data.xy.XYSeries;
import stratx.BackTest;
import stratx.utils.Candlestick;
import stratx.utils.PriceHistory;
import stratx.utils.Signal;

import java.awt.*;

public class WMA extends Indicator implements IIndicator {
    private final int period;

    private final PriceHistory priceHistory;

    // @TODO make this configurable
    private final boolean SHOW_ON_CHART = true;
    private final Color COLOR = new Color(0xFFBF41);
    private final float LINE_WIDTH = 2.0F;

    private XYSeries wmaLine;


    public WMA(BackTest simulation, int period) {
        super(simulation);
        this.period = period;
        this.priceHistory = new PriceHistory(period);
    }

    @Override
    public void update(Candlestick candle) {
        priceHistory.add(candle);

        if (simulation.isShowGUI() && SHOW_ON_CHART && priceHistory.length() >= period) {
            if (wmaLine == null) wmaLine = simulation.getGUI().getChartRenderer().addEMALine(COLOR, LINE_WIDTH);
            else wmaLine.add(candle.getDate(), getWMA());
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
}
