package stratx.modes;

import stratx.strategies.Strategy;
import stratx.utils.Candlestick;

// @TODO Logging/trace log
public class Simulation extends Mode {
    public Simulation(Strategy strategy, String coin) {
        super(Type.SIMULATION, strategy, coin);
        Runtime.getRuntime().addShutdownHook(new Thread(this::onExit));
    }

    @Override
    public void start() {}

    @Override
    protected void onPriceUpdate(double prevPrice, double newPrice) {
//        System.out.println("Price Change: $" + MathUtils.COMMAS_2F.format((newPrice - prevPrice)));
        this.checkTakeProfitStopLoss();
    }

    @Override
    protected void onCandleClose(Candlestick candle) {
        System.out.println("Candle Close: " + candle.getClose());
        this.checkTakeProfitStopLoss();
        this.checkBuySellSignals(candle);
        System.out.println("Bal: $" +ACCOUNT.getBalance());
    }

    private void onExit() {
        if (strategy.CLOSE_OPEN_TRADES_ON_EXIT) this.closeOpenTrades();
        this.printResults();
    }
}
