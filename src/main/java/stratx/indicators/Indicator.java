package stratx.indicators;


import stratx.utils.Candlestick;
import stratx.utils.PriceHistory;
import stratx.utils.Signal;

public abstract class Indicator {
    protected PriceHistory priceHistory = null;

    public abstract void update(Candlestick candle);

    public abstract Signal getSignal();

    public PriceHistory getPriceHistory() {
        return priceHistory;
    }
}
