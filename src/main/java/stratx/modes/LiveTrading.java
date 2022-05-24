package stratx.modes;

import stratx.StratX;
import stratx.strategies.Strategy;
import stratx.utils.Candlestick;
import stratx.utils.CurrencyPair;
import stratx.utils.MathUtils;

public class LiveTrading extends Mode {
    public LiveTrading(Strategy strategy, CurrencyPair coin) {
        super(Type.LIVE, strategy, coin);
    }

    @Override
    protected void start() {
        StratX.both("LiveTrading mode started with balance of $" + ACCOUNT.getBalance());
    }

    @Override
    protected void onPriceUpdate(double prevPrice, double newPrice) {
        this.checkTakeProfitStopLoss();
    }

    @Override
    protected void onCandleClose(Candlestick candle) {
        this.checkTakeProfitStopLoss();
        this.checkBuySellSignals(candle);
        StratX.both("Candle Close {} - Bal: ${}", MathUtils.roundTwoDec(candle.getClose()), MathUtils.roundTwoDec(ACCOUNT.getBalance()));
    }
}
