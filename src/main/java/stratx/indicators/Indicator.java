package stratx.indicators;


import stratx.utils.Candlestick;
import stratx.utils.Signal;

public abstract class Indicator {
    public abstract void update(Candlestick candle);

    public abstract Signal getSignal();
}
