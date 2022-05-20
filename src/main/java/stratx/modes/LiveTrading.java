package stratx.modes;

import stratx.strategies.Strategy;
import stratx.utils.Candlestick;
import stratx.utils.MathUtils;

public class LiveTrading extends Mode {
    public LiveTrading(Strategy strategy, String coin) {
        super(Type.LIVE, strategy, coin);
    }

    @Override
    protected void start() {
        LOGGER.info("LiveTrading mode started with balance of $" + ACCOUNT.getBalance());
    }

    @Override
    protected void onPriceUpdate(double prevPrice, double newPrice) {
        this.checkTakeProfitStopLoss();
    }

    @Override
    protected void onCandleClose(Candlestick candle) {
        this.checkTakeProfitStopLoss();
        this.checkBuySellSignals(candle);
        LOGGER.info("Candle Close {} - Bal: ${}", MathUtils.roundTwoDec(candle.getClose()), MathUtils.roundTwoDec(ACCOUNT.getBalance()));
    }
}
