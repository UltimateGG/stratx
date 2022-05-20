package stratx.strategies;

import stratx.utils.Candlestick;
import stratx.utils.Signal;

public class GridTrading extends Strategy {
    private final double gridSize;
    private double baseLine = 0;
    private Signal currentSignal = Signal.HOLD;

    public GridTrading(double gridSize) {
        this("grid.yml", gridSize);
    }

    public GridTrading(String configFile, double gridSize) {
        super("Grid Trading", configFile);
        this.gridSize = gridSize;
    }

    @Override
    public void onPriceUpdate(double prevPrice, double newPrice) {
        if (baseLine == 0) baseLine = newPrice;
    }

    @Override
    public void onCandleClose(Candlestick candle) {
        if (baseLine == 0) baseLine = candle.getClose();

        int gridJumps = (int) Math.floor((candle.getClose() - baseLine) / gridSize);
        if (gridJumps > 0) {
            currentSignal = Signal.SELL;
        } else if (gridJumps < 0) {
            currentSignal = Signal.BUY;
        } else {
            currentSignal = Signal.HOLD;
        }

        if (Math.abs(gridJumps) >= 3) // Move baseline
            baseLine = candle.getClose();
    }

    @Override
    public Signal getSignal() {
        return currentSignal;
    }
}
