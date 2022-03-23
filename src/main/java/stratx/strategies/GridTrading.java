package stratx.strategies;

import stratx.BackTest;
import stratx.utils.Candlestick;
import stratx.utils.Signal;

public class GridTrading extends Strategy {
    private final double gridSize = 40;
    private double baseLine = 0;
    private Signal currentSignal = Signal.HOLD;

    public GridTrading(BackTest simulation) {
        super("Grid Trading", simulation);
        // This is where you configure the strategy
        this.BUY_AMOUNT_PERCENT = 75.0;
        this.USE_STOP_LOSS = false;
        this.TAKE_PROFIT = 10.0;
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
