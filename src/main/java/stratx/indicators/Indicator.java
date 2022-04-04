package stratx.indicators;


import stratx.modes.Mode;
import stratx.utils.Candlestick;
import stratx.utils.Signal;

public abstract class Indicator {
    protected final Mode mode;

    public Indicator(Mode mode) {
        this.mode = mode;
    }

    public abstract void update(Candlestick candle);

    public abstract Signal getSignal();
}
