package stratx.strategies;

import stratx.BackTest;
import stratx.utils.Candlestick;
import stratx.utils.Signal;

public class GridTrading extends Strategy {
    private final double gridSize;
    private double baseLine = 0;
    private Signal currentSignal = Signal.HOLD;

    public GridTrading(BackTest simulation, double gridSize) {
        this(simulation, "grid.yml", gridSize);
    }

    public GridTrading(BackTest simulation, String configFile, double gridSize) {
        super("Grid Trading", simulation, configFile);
        this.gridSize = gridSize;
    }

    @Override
    public void update(Candlestick candle) {
        if (baseLine == 0) baseLine = candle.getClose();

        int gridJumps = (int) Math.floor((candle.getClose() - baseLine) / gridSize);
        if (gridJumps > 0) {
            currentSignal = Signal.BUY;
        } else if (gridJumps < 0) {
            currentSignal = Signal.SELL;
        } else {
            currentSignal = Signal.HOLD;
        }

        if (Math.abs(gridJumps) >= 5) // Move baseline
            baseLine = candle.getClose();
    }

    @Override
    public Signal getSignal() {
        return currentSignal;
    }
}
