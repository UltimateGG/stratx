package stratx;

import stratx.gui.BacktestGUI;
import stratx.strategies.GridTrading;
import stratx.strategies.Strategy;
import stratx.utils.*;

import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class BackTest {
    private final String PRICE_DATA = "src/main/resources/downloader/RVNUSDT_15m_3.26.2021.strx";
    private final boolean SHOW_GUI = true; // Disable for way faster performance
    private final double STARTING_BALANCE = 100;

    private final Account account = new Account(STARTING_BALANCE);
    private List<Candlestick> data;
    private BacktestGUI GUI;
    private Candlestick currentCandle;

    public static void main(String... args) {
        // Load the price data in
        BackTest simulation = new BackTest();
        simulation.loadData(simulation.PRICE_DATA);

        Strategy gridStrat = new GridTrading(simulation, 0.007142857);

        try {
            simulation.runTest(gridStrat);
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

        StratX.log("Loader has successfully loaded %s data points in %dms", MathUtils.COMMAS.format(data.size()), System.currentTimeMillis() - start);
    }

    /** Returns the % profit this run */
    private double runTest(Strategy strategy) {
        if (SHOW_GUI) GUI = new BacktestGUI(PRICE_DATA, 1800, 900);
        account.reset();
        StratX.log("Running test with a starting balance of $%s\n", MathUtils.COMMAS.format(STARTING_BALANCE));
        System.out.println();
        StratX.log("-- Begin --");

        int index = 0;
        for (Candlestick candle : data) {
            currentCandle = candle;
            if (SHOW_GUI) GUI.getChartRenderer().addCandle(candle, index == data.size() - 1);

            this.checkBuySellSignals(candle, strategy);
            this.checkTakeProfitStopLoss(candle, strategy);

            index++;
        }

        StratX.log("-- End --");

        if (strategy.CLOSE_OPEN_TRADES_ON_EXIT)
            this.closeOpenTrades();

        printResults(strategy);
        if (SHOW_GUI) GUI.show(); // Show GUI at end for performance reasons and candles arent added to chart until end anyways
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
        System.out.printf("[!] Final Balance: $%s USD %s (%d trade%s made)\n",
                MathUtils.COMMAS_2F.format(account.getBalance()),
                MathUtils.getPercent(account.getBalance() - STARTING_BALANCE, STARTING_BALANCE),
                account.getTrades().size(),
                account.getTrades().size() == 1 ? "" : "s");

        if (account.getTrades().size() == 0) return;
        Trade bestTradeProfit = account.getTrades().get(0);
        Trade bestTradePercent = account.getTrades().get(0);
        Trade worstTradeProfit = account.getTrades().get(0);
        Trade worstTradePercent = account.getTrades().get(0);
        int winningTrades = 0;
        int losingTrades = 0;

        for (Trade trade : account.getTrades()) {
            if (trade.getProfitPercent() > bestTradePercent.getProfitPercent()) bestTradePercent = trade;
            if (trade.getProfitPercent() < worstTradePercent.getProfitPercent()) worstTradePercent = trade;
            if (trade.getProfit() > bestTradeProfit.getProfit()) bestTradeProfit = trade;
            if (trade.getProfit() < worstTradeProfit.getProfit()) worstTradeProfit = trade;

            if (trade.getProfitPercent() > 0.0) winningTrades++;
            else losingTrades++;
        }

        System.out.printf("[!] Winning trades: %s (%s)\n", MathUtils.COMMAS.format(winningTrades), MathUtils.getPercent(winningTrades, account.getTrades().size()));
        System.out.printf("[!] Losing trades: %s (%s)\n", MathUtils.COMMAS.format(losingTrades), MathUtils.getPercent(losingTrades, account.getTrades().size()));
        System.out.println("\n-- Best trade --");
        System.out.println("By $: " + bestTradeProfit);
        System.out.println("By %: " + bestTradePercent);
        System.out.println("-- Worst trade --");
        System.out.println("By $: " + worstTradeProfit);
        System.out.println("By %: " + worstTradePercent);
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
