package stratx.indicators;

import stratx.utils.Candlestick;
import stratx.utils.Signal;

public interface Indicator {
    String getName();

    void update(Candlestick candle);

    Signal getSignal();
}
