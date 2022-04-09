package stratx.modes;

import stratx.strategies.Strategy;
import stratx.utils.Candlestick;

// @TODO
public class LiveTrading extends Mode {
    public LiveTrading(Strategy strategy, String coin) {
        super(Type.LIVE, strategy, coin);
    }

    @Override
    protected void start() {

    }

    @Override
    protected void onPriceUpdate(double prevPrice, double newPrice) {

    }

    @Override
    protected void onCandleClose(Candlestick candle) {

    }
}
