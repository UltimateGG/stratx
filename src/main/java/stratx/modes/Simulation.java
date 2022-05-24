package stratx.modes;

import stratx.StratX;
import stratx.strategies.Strategy;
import stratx.utils.Candlestick;
import stratx.utils.CurrencyPair;

public class Simulation extends Mode {
    public Simulation(Strategy strategy, CurrencyPair coin) {
        super(Type.SIMULATION, strategy, coin);
        Runtime.getRuntime().addShutdownHook(new Thread(this::onExit));
    }

    @Override
    public void start() {}

    @Override
    protected void onPriceUpdate(double prevPrice, double newPrice) {
        this.checkTakeProfitStopLoss();
    }

    @Override
    protected void onCandleClose(Candlestick candle) {
        this.checkTakeProfitStopLoss();
        this.checkBuySellSignals(candle);
        StratX.both("Candle Close - Bal: $" + ACCOUNT.getBalance());
    }

    private void onExit() {
        if (strategy.CLOSE_OPEN_TRADES_ON_EXIT) this.closeOpenTrades();
        this.printResults();
    }
}
