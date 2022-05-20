package stratx.indicators;

import stratx.utils.Candlestick;
import stratx.utils.Signal;

public class HeikinAshiTrends extends Indicator {
    private boolean wasLastBullish = false;
    private Signal signal = Signal.HOLD;


    public HeikinAshiTrends() {
        super("HA Trends");
    }

    @Override
    public void update(Candlestick candle) {
        CandleType direction = (candle.getClose() < candle.getOpen()) ? CandleType.BEARISH : CandleType.BULLISH;
        boolean noLowerWick = candle.getOpen() <= candle.getLow();
        boolean noUpperWick = candle.getOpen() >= candle.getHigh();
        boolean isBullish = direction == CandleType.BULLISH && noLowerWick;
        boolean isBearish = direction == CandleType.BEARISH && noUpperWick;

        // Sell takes priority
        signal = (isBearish) ? Signal.SELL
                : (isBullish && wasLastBullish) ? Signal.BUY
                : Signal.HOLD;

        wasLastBullish = isBullish;
    }

    /** Get the strength of a candle
     * The bigger the body size = more
     * The smaller the wick = more */
    private double getCandleStrength(Candlestick candle, CandleType type) {
        double height = Math.abs(candle.getHigh() - candle.getLow());
        double bodyPercent = type == CandleType.BULLISH ?
                (candle.getClose() - candle.getOpen()) :
                (candle.getOpen() - candle.getClose());
        bodyPercent /= height;
        double wickPercent = type == CandleType.BULLISH
                ? (candle.getHigh() - candle.getClose())
                : (candle.getClose() - candle.getLow());
        wickPercent /= height;
//        System.out.println("Body Percent: " + bodyPercent + " Wick Percent: " + wickPercent + " " + (bodyPercent + wickPercent));
        return (type == CandleType.BEARISH ? -1 : 1) * (bodyPercent + wickPercent);
    }

    @Override
    public Signal getSignal() {
        return signal;
    }

    private enum CandleType { BEARISH, BULLISH, NEUTRAL }
}
