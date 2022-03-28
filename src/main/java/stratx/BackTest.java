package stratx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.data.xy.XYSeries;
import stratx.gui.BacktestGUI;
import stratx.strategies.GridTrading;
import stratx.strategies.Strategy;
import stratx.utils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class BackTest {
    static { org.fusesource.jansi.AnsiConsole.systemInstall(); } // @TODO Move to main entry point
    private final Logger LOGGER = LogManager.getLogger("BackTest");
    private final Configuration CONFIG = new Configuration("config/config.yml");
    private final String PRICE_DATA = "src/main/resources/downloader/RVNUSDT_15m_3.26.2021.strx";
    private final double STARTING_BALANCE = CONFIG.getDouble("backtest.starting-balance", 100.0);
    private final boolean SHOW_SIGNALS = CONFIG.getBoolean("backtest.show-signals", true);
    private final boolean SHOW_GUI = true;

    private final Account ACCOUNT = new Account(STARTING_BALANCE);
    private List<Candlestick> data;
    private BacktestGUI GUI;
    private Candlestick currentCandle;
    private final ArrayList<XYSeries> INDICATOR_LOCKS = new ArrayList<>();

    public static void main(String... args) {
        BackTest simulation = new BackTest();
        simulation.loadData(simulation.PRICE_DATA); // Load the price data in

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

        LOGGER.info("Loader has successfully loaded {} data points in {}ms", MathUtils.COMMAS.format(data.size()), System.currentTimeMillis() - start);
    }

    /** Returns the % profit this run */
    private double runTest(Strategy strategy) {
        if (SHOW_GUI) GUI = new BacktestGUI(PRICE_DATA, this, 1800, 900);
        ACCOUNT.reset();

        StratX.trace(PRICE_DATA.substring(PRICE_DATA.lastIndexOf('/') + 1));
        StratX.trace("Starting backtest at {} ({}) on {} candles...", new Date(), System.currentTimeMillis(), data.size());
        StratX.trace("Starting balance: ${}", MathUtils.COMMAS.format(STARTING_BALANCE));
        StratX.trace(strategy.toString());

        LOGGER.info("Running test with a starting balance of ${}\n\n", MathUtils.COMMAS.format(STARTING_BALANCE));
        LOGGER.info("-- Begin --");

        int index = 0;
        for (Candlestick candle : data) {
            boolean isLast = index == data.size() - 1;
            currentCandle = candle;

            if (SHOW_GUI) GUI.getChartRenderer().addCandle(candle, isLast);

            this.checkBuySellSignals(candle, strategy);
            this.checkTakeProfitStopLoss(candle, strategy);

            if (index % 1000 == 0 && index > 0)
                LOGGER.info("-- Backtest: {}% --", Math.round((double)index / data.size() * 100.0D));

            index++;
        }

        for (XYSeries plot : INDICATOR_LOCKS) {
            plot.setNotify(true);
            plot.fireSeriesChanged();
        }

        LOGGER.info("-- End --");

        if (strategy.CLOSE_OPEN_TRADES_ON_EXIT)
            this.closeOpenTrades();

        printResults(strategy);
        if (SHOW_GUI) GUI.show(); // Show GUI at end for performance reasons and candles arent added to chart until end anyways
        return ((ACCOUNT.getBalance() - STARTING_BALANCE) / STARTING_BALANCE) * 100.0D;
    }

    // Places buy/sell orders (Signals on chart as of now)
    private void checkBuySellSignals(Candlestick candle, Strategy strategy) {
        strategy.update(candle);
        Signal signal = strategy.getSignal();

        if (signal == Signal.BUY) {
            double amt = strategy.getBuyAmount();
            if (amt > 0 && amt >= strategy.MIN_USD_PER_TRADE)
                ACCOUNT.openTrade(new Trade(this, candle, amt));
        } else if (signal == Signal.SELL) {
            for (Trade trade : ACCOUNT.getTrades()) {
                if (!trade.isOpen()) continue;
                ACCOUNT.closeTrade(trade, candle, "Indicator Signal");
                //break; // @TODO This closes one trade and then breaks, should it close all? Config opt?
            }
        }
    }

    // Take profit, stop loss, & trailing stop loss check
    private void checkTakeProfitStopLoss(Candlestick candle, Strategy strategy) {
        if (ACCOUNT.getOpenTrades() == 0) return;
        for (Trade trade : ACCOUNT.getTrades()) {
            if (!trade.isOpen()) continue;
            double profitPercent = trade.getProfitPercent();
            boolean takeProfit = strategy.USE_TAKE_PROFIT && profitPercent >= strategy.TAKE_PROFIT;
            boolean stopLoss = strategy.USE_STOP_LOSS && profitPercent <= -strategy.STOP_LOSS;

            if (takeProfit || stopLoss) {
                ACCOUNT.closeTrade(trade, candle, takeProfit ? "Take Profit" : "Stop Loss");
            } else if (strategy.USE_TRAILING_STOP) {
                if (profitPercent >= strategy.ARM_TRAILING_STOP_AT) trade.setTrailingStopArmed(true);
                if (trade.isTrailingStopArmed()) {
                    double profitDiff = profitPercent - trade.getLastProfitPercent();

                    if (profitDiff <= -strategy.TRAILING_STOP && profitPercent > 0.0)
                        ACCOUNT.closeTrade(trade, candle, "Trailing Stop");
                    trade.setLastProfitPercent(profitPercent);
                }
            }

            trade.setLastProfitPercent(profitPercent);
        }
    }

    private void closeOpenTrades() {
        LOGGER.info(" ");
        LOGGER.info("-- Closing any remaining open trades --");
        StratX.trace("-- Closing any remaining open trades --");
        int closed = 0;

        for (Trade trade : ACCOUNT.getTrades()) {
            if (!trade.isOpen()) continue;
            ACCOUNT.closeTrade(trade, currentCandle, "Closing on exit");
            closed++;
        }

        LOGGER.info("-- Closed {} open trades --\n", closed);
        StratX.trace("-- Closed {} open trades --\n", closed);
    }

    private void printResults(Strategy strategy) {
        LOGGER.info("-- Results for strategy '{}' --", strategy.name);
        String info = String.format("[!] Final Balance: $%s USD %s (%s trade%s made)",
        MathUtils.COMMAS_2F.format(ACCOUNT.getBalance()),
                MathUtils.getPercent(ACCOUNT.getBalance() - STARTING_BALANCE, STARTING_BALANCE),
                ACCOUNT.getTrades().size(),
                ACCOUNT.getTrades().size() == 1 ? "" : "s");
        LOGGER.info(info);
        StratX.trace(" ");
        StratX.trace("-- End --");
        StratX.trace(info);

        if (ACCOUNT.getTrades().size() == 0) return;
        Trade bestTradeProfit = ACCOUNT.getTrades().get(0);
        Trade bestTradePercent = ACCOUNT.getTrades().get(0);
        Trade worstTradeProfit = ACCOUNT.getTrades().get(0);
        Trade worstTradePercent = ACCOUNT.getTrades().get(0);
        int winningTrades = 0;
        int losingTrades = 0;

        for (Trade trade : ACCOUNT.getTrades()) {
            if (trade.getProfitPercent() > bestTradePercent.getProfitPercent()) bestTradePercent = trade;
            if (trade.getProfitPercent() < worstTradePercent.getProfitPercent()) worstTradePercent = trade;
            if (trade.getProfit() > bestTradeProfit.getProfit()) bestTradeProfit = trade;
            if (trade.getProfit() < worstTradeProfit.getProfit()) worstTradeProfit = trade;

            if (trade.getProfitPercent() > 0.0) winningTrades++;
            else losingTrades++;
        }

        LOGGER.info("[!] Winning trades: {} ({})", MathUtils.COMMAS.format(winningTrades), MathUtils.getPercent(winningTrades, ACCOUNT.getTrades().size()));
        LOGGER.info("[!] Losing trades: {} ({})\n", MathUtils.COMMAS.format(losingTrades), MathUtils.getPercent(losingTrades, ACCOUNT.getTrades().size()));
        LOGGER.info("-- Best trade --");
        LOGGER.info("By $: " + bestTradeProfit);
        LOGGER.info("By %: " + bestTradePercent);
        LOGGER.info("-- Worst trade --");
        LOGGER.info("By $: " + worstTradeProfit);
        LOGGER.info("By %: " + worstTradePercent);
//        if (LOG_TRADES) log.write("[END]," + System.currentTimeMillis() + "," + new Date() + ", Total Trades: " + account.getTrades().size() + ", Bal: $" + account.getBalance()
//            + "," + MathUtils.getPercent(account.getBalance() - STARTING_BALANCE, STARTING_BALANCE));
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public Configuration getConfig() {
        return CONFIG;
    }

    public boolean shouldShowSignals() {
        return SHOW_SIGNALS;
    }

    public boolean isShowGUI() {
        return SHOW_GUI && GUI != null;
    }

    public Account getAccount() {
        return ACCOUNT;
    }

    public BacktestGUI getGUI() {
        return GUI;
    }

    public Candlestick getCurrentCandle() {
        return currentCandle;
    }

    public String getCoin() {
        return PRICE_DATA.substring(PRICE_DATA.lastIndexOf("/") + 1).split("_")[0];
    }

    public void addLock(XYSeries series) {
        series.setNotify(false);
        INDICATOR_LOCKS.add(series);
    }
}
