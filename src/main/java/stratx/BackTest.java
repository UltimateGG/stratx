package stratx;

import stratx.gui.BacktestGUI;
import stratx.strategies.GridTrading;
import stratx.strategies.Strategy;
import stratx.utils.*;

import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class BackTest {
    // Config
    private final String PRICE_DATA = "src/main/resources/BTC-USD_15m.json";
    private final boolean SHOW_GUI = true;
    private final double STARTING_BALANCE = 100;

    private final Account account = new Account(STARTING_BALANCE);
    private List<Candlestick> data;
    private BacktestGUI GUI;
    private Candlestick currentCandle;

    public static void main(String... args) {
        // Load the price data in
        BackTest simulation = new BackTest();
        simulation.loadData(simulation.PRICE_DATA);

        Strategy gridStrat = new GridTrading(simulation);
        //test.indicators.add(new RSI(test,14, 58, 44));

        try {
            double profit = simulation.runTest(gridStrat);
            //StratX.log("Backtest complete, profit: %.2f", profit);
        } catch (Exception e) {
            StratX.error("Caught exception during backtest: ", e);
            System.exit(1);
        }
    }

    private void loadData(String file) {
        long start = System.currentTimeMillis();
        data = Loader.loadData(file);

        if (data.size() == 0) {
            StratX.warn("Failed to load price data, exiting..");
            System.exit(1);
        }

        StratX.log("Loader has successfully loaded %d data points in %dms", data.size(), System.currentTimeMillis() - start);
    }

    /** Returns the % profit this run */
    private double runTest(Strategy strategy) {
        if (SHOW_GUI) {
            GUI = new BacktestGUI(PRICE_DATA, 1800, 900);
            GUI.show();
        }

        account.reset();
        System.out.println();
        StratX.log("-- Begin --");

        int index = 0;
        for (Candlestick candle : data) {
            if (index > 2000) break; // @todo temp
            currentCandle = candle;
            if (SHOW_GUI) GUI.getChartRenderer().addCandle(candle);

            this.checkBuySellSignals(candle, strategy);
            this.checkTakeProfitStopLoss(candle, strategy);

            index++;
        }

        StratX.log("-- End --");

        if (strategy.CLOSE_OPEN_TRADES_ON_EXIT)
            this.closeOpenTrades();

        printResults(strategy);
        return ((account.getBalance() - STARTING_BALANCE) / STARTING_BALANCE) * 100.0D;
    }

    // Places buy/sell orders (Signals on chart as of now)
    private void checkBuySellSignals(Candlestick candle, Strategy strategy) {
        strategy.update(candle);
        Signal signal = strategy.getSignal();

        if (signal == Signal.BUY) {
            double amt = strategy.getBuyAmount();
            if (amt > 0 && amt >= strategy.MIN_USD_PER_TRADE)
                account.openTrade(new Trade(this, candle, amt));
        } else if (signal == Signal.SELL) {
            for (Trade trade : account.getTrades()) {
                if (!trade.isOpen()) continue;
                account.closeTrade(trade, candle, "Indicator Signal");
                //break; // @TODO This closes one trade and then breaks, should it close all? Config opt?
            }
        }
    }

    // Take profit, stop loss, & trailing stop loss check
    private void checkTakeProfitStopLoss(Candlestick candle, Strategy strategy) {
        if (account.getOpenTrades() == 0) return;
        for (Trade trade : account.getTrades()) {
            if (!trade.isOpen()) continue;
            double profitPercent = trade.getProfitPercent();
            boolean takeProfit = profitPercent >= strategy.TAKE_PROFIT;
            boolean stopLoss = strategy.USE_STOP_LOSS && profitPercent <= -strategy.STOP_LOSS;

            if (takeProfit || stopLoss) {
                account.closeTrade(trade, candle, takeProfit ? "Take Profit" : "Stop Loss");
            } else if (strategy.USE_TRAILING_STOP) {
                if (profitPercent >= strategy.ARM_TRAILING_STOP_AT) trade.setTrailingStopArmed(true);
                if (trade.isTrailingStopArmed()) {
                    double profitDiff = profitPercent - trade.getLastProfitPercent();

                    if (profitDiff <= -strategy.TRAILING_STOP && profitPercent > 0.0)
                        account.closeTrade(trade, candle, "Trailing Stop");
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

    private void printResults(Strategy strategy) {
        StratX.log("-- Results for strategy '%s' --", strategy.name);
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
        return SHOW_GUI && GUI != null;
    }

    public BacktestGUI getGUI() {
        return GUI;
    }

    public Candlestick getCurrentCandle() {
        return currentCandle;
    }
}
