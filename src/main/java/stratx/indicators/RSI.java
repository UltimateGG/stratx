package stratx.indicators;

import stratx.utils.Candlestick;
import stratx.utils.MathUtils;
import stratx.utils.PriceHistory;
import stratx.utils.Signal;

public class RSI implements Indicator {
    private final int period;
    private final double overbought;
    private final double oversold;

    private final PriceHistory priceHistory;

    public RSI(int period, double overbought, double oversold) {
        this.period = period;
        this.overbought = overbought;
        this.oversold = oversold;
        this.priceHistory = new PriceHistory(period);
    }

    @Override
    public String getName() {
        return "RSI";
    }

    @Override
    public void update(Candlestick candle) {
        priceHistory.add(candle);
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
