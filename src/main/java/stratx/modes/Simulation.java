package stratx.modes;

import stratx.utils.Candlestick;


public class Simulation extends Mode {
    public Simulation(String coin, boolean showGui) {
        super(Type.SIMULATION, coin, showGui);
    }

    @Override
    public void start() {}

    @Override
    protected void onPriceUpdate(double prevPrice, double newPrice) {
        System.out.println("Price Diff: $" + (newPrice - prevPrice));
    }

    @Override
    protected void onCandleClose(Candlestick candle) {
        System.out.println("Candle closed: " + candle);
    }
}
