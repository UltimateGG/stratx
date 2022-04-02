package stratx.indicators;


import stratx.BackTest;
import stratx.utils.Candlestick;
import stratx.utils.Signal;

public abstract class Indicator {
    protected final BackTest simulation;

    public Indicator(BackTest simulation) {
        this.simulation = simulation;
    }

    public abstract void update(Candlestick candle);

    public abstract Signal getSignal();
}
