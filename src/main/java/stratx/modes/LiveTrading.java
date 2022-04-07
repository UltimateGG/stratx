package stratx.modes;

import stratx.utils.Candlestick;

// @TODO
public class LiveTrading extends Mode {
    public LiveTrading(String coin) {
        super(Type.LIVE, coin);
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
