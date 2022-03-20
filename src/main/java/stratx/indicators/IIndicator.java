package stratx.indicators;

import stratx.utils.Candlestick;
import stratx.utils.Signal;

public interface IIndicator {

    void update(Candlestick candle);

    Signal getSignal();
}
