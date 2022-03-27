package stratx.strategies;

import stratx.BackTest;
import stratx.indicators.IIndicator;
import stratx.utils.Candlestick;
import stratx.utils.LogFile;
import stratx.utils.MathUtils;
import stratx.utils.Signal;

import java.util.ArrayList;
import java.util.Arrays;

public class Strategy implements IIndicator {
    /** The name of the strategy */
    public final String name;

    /** The max open trade positions at once */
    public int MAX_OPEN_TRADES = 1;

    /** The percent of profit a position will sell at if greater than or equal to that percent [0-100]% */
    public double TAKE_PROFIT = 5.0;

    /** Whether to use the stop loss or not */
    public boolean USE_STOP_LOSS = true;

    /** The percent a position will sell at if the loss is greater than or equal to that percent [0-100]% */
    public double STOP_LOSS = 2.0;

    /** Whether to use a trailing stop loss or not */
    public boolean USE_TRAILING_STOP = false;

    /** The percent of profit to enable trailing stop at, once the profit
     * is greater than or equal to this amount, and then it drops by TRAILING_STOP percent,
     * the position will sell, locking in profits [0-100]% */
    public double ARM_TRAILING_STOP_AT = 0.1;

    /** The percent a position will sell at if the profit drops by this percent [0-100]% */
    public double TRAILING_STOP = 0.5;

    /** Whether to use indicator sell signals or not, if false
     * no positions will be sold unless by take profit, stop loss,
     * or trailing stop */
    public boolean SELL_BASED_ON_INDICATORS = true;

    /** Whether to close any remaining open positions before exiting the backtest/simulation or not */
    public boolean CLOSE_OPEN_TRADES_ON_EXIT = true;

    /** The minimum amount of indicators signaling BUY to open a position
     * If -1, this will use the amount of enabled indicators */
    public int MIN_BUY_SIGNALS = -1;

    /** The minimum amount of indicators signaling SELL to close a position
     * If -1, this will use the amount of enabled indicators */
    public int MIN_SELL_SIGNALS = -1;

    /** The maximum amount of USD a trade can be placed for
     * If -1, this will use all available money in the account
     * at that time */
    public double MAX_USD_PER_TRADE = -1;

    /** The minimum about of USD a trade can be placed for
     * If your balance is less than this, no trade is placed */
    public double MIN_USD_PER_TRADE = 1.0;

    /** The percentage of your balance a trade will be placed for
     * Example: 25.0 with $100 balance = trade placed for $25
     * If this turns out to be more than your MAX_USD_PER_TRADE, or
     * MIN_USD_PER_TRADE, those values will be used.
     * If -1, this is disabled and uses the MIN_USD_PER_TRADE */
    public double BUY_AMOUNT_PERCENT = 25.0;

    /** Don't enter a trade if there are more indicators signaling
     * sell than buy */
    public boolean DONT_BUY_IF_SELL_GREATER = true;

    private final BackTest simulation;
    private final ArrayList<IIndicator> indicators = new ArrayList<>();


    public Strategy(String name, BackTest simulation, IIndicator... indicators) {
        this.name = name;
        this.simulation = simulation;
        this.indicators.addAll(Arrays.asList(indicators));
    }

    /** Called every time a candle is closed or every "tick" */
    @Override
    public void update(Candlestick candle) {
        for (IIndicator indicator : indicators)
            indicator.update(candle);
    }

    /** Default implementation, uses the indicators to determine the signal
     * You may override this for custom strategies */
    @Override
    public Signal getSignal() {
        int buySignals = 0;
        int sellSignals = 0;

        for (IIndicator indicator : indicators) {
            Signal signal = indicator.getSignal();
            if (signal == Signal.BUY) buySignals++;
            if (signal == Signal.SELL) sellSignals++;
        }

        if (buySignals == 0 && sellSignals == 0) return Signal.HOLD;

        if (((MIN_BUY_SIGNALS == -1 && buySignals >= indicators.size()) || (buySignals >= MIN_BUY_SIGNALS && MIN_BUY_SIGNALS != -1))
                && (buySignals >= sellSignals && DONT_BUY_IF_SELL_GREATER)
                && (simulation.getAccount().getOpenTrades() < MAX_OPEN_TRADES)
                && (simulation.getAccount().getBalance() > 0)
        ) return Signal.BUY;
        else if (((MIN_SELL_SIGNALS == -1 && sellSignals >= indicators.size()) || (sellSignals >= MIN_SELL_SIGNALS && MIN_SELL_SIGNALS != -1))
                && (SELL_BASED_ON_INDICATORS)
                && (simulation.getAccount().getOpenTrades() > 0)
        ) return Signal.SELL;

        return Signal.HOLD;
    }

    /** Called when a trade is opened to determine how much USD
     * it should be bought for. You may override this for custom implementations. */
    public double getBuyAmount() {
        double bal = simulation.getAccount().getBalance();
        double buy = MIN_USD_PER_TRADE;

        // Percentage buy
        if (BUY_AMOUNT_PERCENT > 0)
            buy = bal * (BUY_AMOUNT_PERCENT / 100.0D);

        return MathUtils.clampDouble(buy, Math.min(MIN_USD_PER_TRADE, bal), MAX_USD_PER_TRADE == -1 ? Double.MAX_VALUE : MAX_USD_PER_TRADE);
    }

    public void addIndicator(IIndicator indicator) {
        indicators.add(indicator);
    }

    public void removeIndicator(IIndicator indicator) {
        indicators.remove(indicator);
    }

    public ArrayList<IIndicator> getIndicators() {
        return indicators;
    }

    public void writeToLog(LogFile log) {
        log.write("MAX_OPEN_TRADES: " + MAX_OPEN_TRADES);
        log.write("TAKE_PROFIT: " + TAKE_PROFIT);
        log.write("USE_STOP_LOSS: " + USE_STOP_LOSS);
        log.write("STOP_LOSS: " + STOP_LOSS);
        log.write("USE_TRAILING_STOP: " + USE_TRAILING_STOP);
        log.write("ARM_TRAILING_STOP_AT: " + ARM_TRAILING_STOP_AT);
        log.write("TRAILING_STOP: " + TRAILING_STOP);
        log.write("SELL_BASED_ON_INDICATORS: " + SELL_BASED_ON_INDICATORS);
        log.write("CLOSE_OPEN_TRADES_ON_EXIT: " + CLOSE_OPEN_TRADES_ON_EXIT);
        log.write("MIN_BUY_SIGNALS: " + MIN_BUY_SIGNALS);
        log.write("MIN_SELL_SIGNALS: " + MIN_SELL_SIGNALS);
        log.write("MAX_USD_PER_TRADE: " + MAX_USD_PER_TRADE);
        log.write("MIN_USD_PER_TRADE: " + MIN_USD_PER_TRADE);
        log.write("BUY_AMOUNT_PERCENT: " + BUY_AMOUNT_PERCENT);
        log.write("DONT_BUY_IF_SELL_GREATER: " + DONT_BUY_IF_SELL_GREATER);
    }
}
