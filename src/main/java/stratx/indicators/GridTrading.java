package stratx.indicators;

import stratx.BackTest;
import stratx.utils.Candlestick;
import stratx.utils.Signal;

public class GridTrading extends Indicator implements IIndicator{
    private final double gridSize = 80;

    private double baseLine = 0;
    private Signal currentSignal = Signal.HOLD;

    public GridTrading(BackTest simulation) {
        super(simulation);
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

        if (Math.abs(gridJumps) >= 5) // Move base line
            baseLine = candle.getClose();
    }

    @Override
    public Signal getSignal() {
        return currentSignal;
    }
}
