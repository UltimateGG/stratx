package stratx;

import stratx.gui.BacktestGUI;
import stratx.indicators.*;
import stratx.utils.*;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class BackTest {
    // Config
    private final String PRICE_DATA = "src/main/resources/BTC-USD_5m.json";
    private final Candlestick.Interval INTERVAL = Candlestick.Interval.FIFTEEN_MINUTES;
    private final boolean SHOW_GUI = true;
    private final boolean AUTO_SCALE = true;
    private final int MAX_CANDLES = 200;

    // Trading Config
    private final double STARTING_BALANCE = 1000;
    private final int MAX_OPEN_TRADES = 1;
    private final double TAKE_PROFIT = 6.50; // Percent
    private final boolean USE_STOP_LOSS = true;
    private final double STOP_LOSS = 1.0;
    private final boolean USE_TRAILING_STOP = true;
    private final double ARM_TRAILING_STOP_AT = 1.0; // Enable when we hit this % of profit
    private final double TRAILING_STOP = 0.35;
    private final boolean SELL_BASED_ON_INDICATORS = true; // Turn off to use stop loss/take profit only
    private final boolean CLOSE_OPEN_TRADES_ON_EXIT = true;
    private final int MIN_BUY_SIGNALS = 2; // -1 = How many indicators enabled at the time
    private final int MIN_SELL_SIGNALS = 2;
    private final double MAX_USD_PER_TRADE = -1; // -1 For entire balance
    private final double MIN_USD_PER_TRADE = 10;
    private final double BUY_AMOUNT_PERCENT = 75.0; // Percent of balance to buy with -1 for disabled (Uses min)

    private final Account account = new Account(STARTING_BALANCE);
    private List<Candlestick> data;
    private BacktestGUI GUI;
    private final ArrayList<IIndicator> indicators = new ArrayList<>();
    private Candlestick currentCandle;

    public static void main(String... args) {
        double bestProfit = 0.0D;
        int bestProfitRun = 0;

        // Load the price data in
        BackTest test = new BackTest();
        test.loadData();

        for (int i = 0; i < 1; i++) {
            test.indicators.clear();
            test.account.reset();

            // Enabled indicators
            test.indicators.add(new RSI(test,14, 58, 44));
            test.indicators.add(new WMA(test, 10));
            test.indicators.add(new EMA(test, 10));
            //test.indicators.add(new SupportResistance(50.0D));
            test.indicators.add(new Test());
            //test.indicators.add(new DojiTest());

            StratX.log("Running backtest #%d...", (i + 1));

            try {
                double profit = test.runTest();
                StratX.log("Backtest #%d complete, profit: %.2f", (i + 1), profit);
                if (profit > bestProfit) {
                    bestProfit = profit;
                    bestProfitRun = i;
                }
            } catch (Exception e) {
                StratX.error("Caught exception during backtest: ", e);
                System.exit(1);
            }
        }

        StratX.log("Best profit: %.2f #%d", bestProfit, bestProfitRun);
    }

    private void loadData() {
        long start = System.currentTimeMillis();
        data = Loader.loadData(PRICE_DATA);

        if (data.size() == 0) {
            StratX.warn("Failed to load price data, exiting..");
            System.exit(1);
        }

        StratX.log("Loader has successfully loaded %d data points in %dms", data.size(), System.currentTimeMillis() - start);
    }

    /** Returns the % profit this run */
    private double runTest() {
        if (SHOW_GUI) {
            GUI = new BacktestGUI(PRICE_DATA, INTERVAL, 1800, 900);
            //gui.populate(data, AUTO_SCALE, MAX_CANDLES);
            GUI.show();
        }

        System.out.println();
        StratX.log("-- Begin --");

        int index = 0;
        for (Candlestick candle : data) {
            if (index > 2000) break; // @todo temp
            currentCandle = candle;
            if (SHOW_GUI) GUI.getChartRenderer().addCandle(candle);

            this.checkBuySellSignals(candle);
            this.checkTakeProfitStopLoss(candle);

            index++;
        }

        StratX.log("-- End --");

        if (CLOSE_OPEN_TRADES_ON_EXIT)
            this.closeOpenTrades();

        printResults();

        if (SHOW_GUI && false) { // Clean up & close GUI
            GUI.dispose();
            GUI = null;
        }

        return ((account.getBalance() - STARTING_BALANCE) / STARTING_BALANCE) * 100.0D;
    }

    // Places buy/sell orders (Signals on chart as of now)
    private void checkBuySellSignals(Candlestick candle) {
        int buySignals = 0;
        int sellSignals = 0;
        for (IIndicator indicator : indicators) {
            indicator.update(candle);

            Signal signal = indicator.getSignal();
            if (signal == Signal.BUY) buySignals++;
            if (signal == Signal.SELL) sellSignals++;
        }

        if (buySignals == 0 && sellSignals == 0) return;

        // Make sure if more indicators are saying sell, don't enter a buy trade
        if ( // TODO Option for "&& (buySignals >= sellSignals)" ? ^^
                ((MIN_BUY_SIGNALS == -1 && buySignals >= indicators.size()) || (buySignals >= MIN_BUY_SIGNALS && MIN_BUY_SIGNALS != -1))
                && (buySignals >= sellSignals)
                && (account.getOpenTrades() < MAX_OPEN_TRADES)
                && (account.getBalance() > 0)
        ) {
            account.openTrade(new Trade(this, candle, getBuyAmount()));
        } else if (
                ((MIN_SELL_SIGNALS == -1 && sellSignals >= indicators.size()) || (sellSignals >= MIN_SELL_SIGNALS && MIN_SELL_SIGNALS != -1))
                && (SELL_BASED_ON_INDICATORS)
                && (account.getOpenTrades() > 0)
        ) {
            for (Trade trade : account.getTrades()) {
                if (!trade.isOpen()) continue;
                account.closeTrade(trade, candle, "Indicator Signal");
            }
        }
    }

    private double getBuyAmount() {
        double bal = account.getBalance();
        double buy = MIN_USD_PER_TRADE;

        // Percentage buy
        if (BUY_AMOUNT_PERCENT > 0)
            buy = bal * (BUY_AMOUNT_PERCENT / 100.0D);

        return MathUtils.clampDouble(buy, Math.min(MIN_USD_PER_TRADE, bal), MAX_USD_PER_TRADE == -1 ? Double.MAX_VALUE : MAX_USD_PER_TRADE);
    }

    // Take profit, stop loss, & trailing stop loss check
    private void checkTakeProfitStopLoss(Candlestick candle) {
        if (account.getOpenTrades() == 0) return;
        for (Trade trade : account.getTrades()) {
            if (!trade.isOpen()) continue;
            double profitPercent = trade.getProfitPercent();
            boolean takeProfit = profitPercent >= TAKE_PROFIT;
            boolean stopLoss = USE_STOP_LOSS && profitPercent <= -STOP_LOSS;

            if (takeProfit || stopLoss) {
                account.closeTrade(trade, candle, takeProfit ? "Take Profit" : "Stop Loss");
            } else if (USE_TRAILING_STOP) {
                if (profitPercent >= ARM_TRAILING_STOP_AT) trade.setTrailingStopArmed(true);
                if (trade.isTrailingStopArmed()) {
                    double profitDiff = profitPercent - trade.getLastProfitPercent();
                    if (profitDiff <= -TRAILING_STOP) account.closeTrade(trade, candle, "Trailing Stop");
                    trade.setLastProfitPercent(profitPercent);
                }
            }

            trade.setLastProfitPercent(profitPercent);
        }
    }

    private void closeOpenTrades() {
        System.out.println();
        StratX.log("-- Closing any remaining open trades --");
        int closed = 0;

        for (Trade trade : account.getTrades()) {
            if (!trade.isOpen()) continue;
            account.closeTrade(trade, currentCandle, "Closing on exit");
            closed++;
        }

        StratX.log("-- Closed %d open trades --\n", closed);
    }

    private void printResults() {
        System.out.println("[!] Final Balance: $" + MathUtils.roundTwoDec(account.getBalance()) + " USD "
                + (MathUtils.getPercent(account.getBalance() - STARTING_BALANCE, STARTING_BALANCE))
                + " (" + account.getTrades().size() + " trade" + (account.getTrades().size() == 1 ? "" : "s") + " made)");

        Trade bestTrade = null;
        Trade worstTrade = null;
        for (Trade trade : account.getTrades()) {
            if (bestTrade == null || trade.getProfitPercent() > bestTrade.getProfitPercent()) bestTrade = trade;
            if (worstTrade == null || trade.getProfitPercent() < worstTrade.getProfitPercent()) worstTrade = trade;
        }

        System.out.print("Best trade: ");
        System.out.println(bestTrade);
        System.out.print("Worst trade: ");
        System.out.println(worstTrade);
    }

    public Account getAccount() {
        return account;
    }

    public boolean isShowGUI() {
        return SHOW_GUI;
    }

    public BacktestGUI getGUI() {
        return GUI;
    }

    public Candlestick getCurrentCandle() {
        return currentCandle;
    }
}
