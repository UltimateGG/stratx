package stratx.indicators;

import stratx.utils.Candlestick;
import stratx.utils.PriceHistory;
import stratx.utils.Signal;

public class EMA implements Indicator {
    private final int period;

    private final PriceHistory priceHistory;
    private double previousEMA = Double.NaN;

    public EMA(int period) {
        this.period = period;
        this.priceHistory = new PriceHistory(period);
    }

    @Override
    public String getName() {
        return "EMA";
    }

    @Override
    public void update(Candlestick candle) {
        priceHistory.add(candle);

        if (priceHistory.length() < period) return;
        previousEMA = getEMA(candle);
    }

    @Override
    public Signal getSignal() {
        Candlestick last = priceHistory.get(priceHistory.length() - 1);
        double ema = getEMA(last);
        if (ema > last.getClose()) return Signal.BUY;
        if (ema < last.getClose()) return Signal.SELL;
        return Signal.HOLD;
    }

    private double getEMA(Candlestick current) {
        if (Double.isNaN(previousEMA)) {
            previousEMA = current.getClose();
            return previousEMA;
        }

        double k = 2.0 / (period + 1);
        return k * (current.getClose() - previousEMA) + previousEMA;
    }
}
