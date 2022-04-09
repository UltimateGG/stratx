package stratx.indicators;

import stratx.utils.Candlestick;
import stratx.utils.Signal;

public class Test extends Indicator { // @TODO
    private Candlestick last;
    private boolean wasLastBullish = false;
    private boolean wasLastBearish = false;
    private Signal signal = Signal.HOLD;

    @Override
    public void update(Candlestick candle) {
        boolean noLowerWick = candle.getOpen() <= candle.getLow();
        boolean noUpperWick = candle.getOpen() >= candle.getHigh();
        boolean isBullish = candle.getClose() > candle.getOpen() && noLowerWick;
        boolean isBearish = (candle.getClose() < candle.getOpen() && noUpperWick);

        // Sell takes priority
        signal = (isBearish) ? Signal.SELL
                : (isBullish && wasLastBullish) ? Signal.BUY
                : Signal.HOLD;

        last = candle;
        wasLastBullish = isBullish;
        wasLastBearish = isBearish;
    }

    @Override
    public Signal getSignal() {
        return signal;
    }
}
