package stratx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import stratx.gui.BacktestGUI;
import stratx.strategies.Strategy;
import stratx.utils.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class BackTest {
    private final Logger LOGGER = LogManager.getLogger("BackTest");
    private final Configuration CONFIG = new Configuration("config\\config.yml");
    private final String PRICE_DATA;
    private final double STARTING_BALANCE = CONFIG.getDouble("backtest.starting-balance", 100.0);
    private final boolean SHOW_SIGNALS = CONFIG.getBoolean("backtest.show-signals", true);
    private final boolean SHOW_GUI;

    private final Account ACCOUNT = new Account(STARTING_BALANCE);
    private List<Candlestick> data;
    private BacktestGUI GUI;
    private Candlestick currentCandle;
    private final ArrayList<XYSeries> INDICATOR_LOCKS = new ArrayList<>(); // @TODO Temp kinda trash solution?


    public BackTest(String priceData, boolean showGui) {
        this.PRICE_DATA = priceData;
        this.SHOW_GUI = showGui;

        this.loadData(this.PRICE_DATA); // Load the price data in
    }

    public void begin(Strategy strategy) {
        try {
            this.runTest(strategy);
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

        StratX.trace(PRICE_DATA.substring(PRICE_DATA.lastIndexOf('\\') + 1));
        StratX.trace("Starting backtest at {} ({}) on {} candles...", new Date(), System.currentTimeMillis(), data.size());
        StratX.trace("Starting balance: ${}", MathUtils.COMMAS.format(STARTING_BALANCE));
        StratX.trace(strategy.toString());

        LOGGER.info("Running test with a starting balance of ${}\n\n", MathUtils.COMMAS.format(STARTING_BALANCE));
        LOGGER.info("-- Begin --");

        ArrayList<Double> balances = new ArrayList<>();

        int index = 0;
        for (Candlestick candle : data) {
            boolean isLast = index == data.size() - 1;
            currentCandle = candle;

            if (SHOW_GUI) GUI.getChartRenderer().addCandle(candle, isLast);

            int pre = ACCOUNT.getOpenTrades();
            this.checkTakeProfitStopLoss(strategy);
            this.checkBuySellSignals(candle, strategy);

            if (index % 1000 == 0 && index > 0)
                LOGGER.info("-- Backtest: {}% --", Math.round((double)index / data.size() * 100.0D));

            if (ACCOUNT.getOpenTrades() < pre)
            balances.add(ACCOUNT.getBalance());

            index++;
        }

        for (XYSeries plot : INDICATOR_LOCKS) {
            plot.setNotify(true);
            plot.fireSeriesChanged();
        }

        temp(balances);
        LOGGER.info("-- End --");

        if (strategy.CLOSE_OPEN_TRADES_ON_EXIT)
            this.closeOpenTrades();

        printResults(strategy);
        if (SHOW_GUI) GUI.show(); // Show GUI at end for performance reasons and candles arent added to chart until end anyways
        return ((ACCOUNT.getBalance() - STARTING_BALANCE) / STARTING_BALANCE) * 100.0D;
    }

    private void temp(ArrayList<Double> balances) {
        // show line chart
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("Balance");
        for (int i = 0; i < balances.size(); i++) {
            series.add(i, balances.get(i));
        }
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Balance", "Candle", "Balance", dataset, PlotOrientation.VERTICAL, true, true, false);
        ChartFrame frame = new ChartFrame("Balance", chart);
        frame.setVisible(true);
        frame.setSize(1800, 600);
    }

    // Places buy/sell orders (Signals on chart as of now)
    private void checkBuySellSignals(Candlestick candle, Strategy strategy) {
        strategy.update(candle);
        Signal signal = strategy.getSignal();

        if (signal == Signal.BUY) {
            double amt = strategy.getBuyAmount();
            if (amt > 0 && ACCOUNT.getBalance() >= amt && strategy.isValidBuy(amt))
                ACCOUNT.openTrade(new Trade(this, amt));
        } else if (signal == Signal.SELL) {
            for (Trade trade : ACCOUNT.getTrades()) {
                if (!trade.isOpen() || !strategy.isValidSell()) continue;
                ACCOUNT.closeTrade(trade, "Indicator Signal");
                if (strategy.SELL_ALL_ON_SIGNAL) break;
            }
        }
    }

    // Take profit, stop loss, & trailing stop loss check
    private void checkTakeProfitStopLoss(Strategy strategy) {
        if (ACCOUNT.getOpenTrades() == 0) return;
        for (Trade trade : ACCOUNT.getTrades()) {
            if (!trade.isOpen()) continue;
            double profitPercent = trade.getProfitPercent();
            boolean takeProfit = strategy.USE_TAKE_PROFIT && profitPercent >= strategy.TAKE_PROFIT;
            boolean stopLoss = strategy.USE_STOP_LOSS && profitPercent <= -strategy.STOP_LOSS;

            if (takeProfit || stopLoss) {
                ACCOUNT.closeTrade(trade, takeProfit ? "Take Profit" : "Stop Loss");
            } else if (strategy.USE_TRAILING_STOP) {
                if (profitPercent >= strategy.ARM_TRAILING_STOP_AT) trade.setTrailingStopArmed(true);
                if (trade.isTrailingStopArmed()) {
                    double profitDiff = profitPercent - trade.getLastProfitPercent();

                    if (profitDiff <= -strategy.TRAILING_STOP && profitPercent > 0.0)
                        ACCOUNT.closeTrade(trade, "Trailing Stop");
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
            ACCOUNT.closeTrade(trade, "Closing on exit");
            closed++;
        }

        LOGGER.info("-- Closed {} open trades --\n", closed);
        StratX.trace("-- Closed {} open trades --\n", closed);
    }

    private void printResults(Strategy strategy) {
        LOGGER.info("-- Results for strategy '{}' on {} --", strategy.name, getCoin());
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
        long avgHoldingTime = 0;
        Trade bestTradeProfit = ACCOUNT.getTrades().get(0);
        Trade bestTradePercent = ACCOUNT.getTrades().get(0);
        Trade worstTradeProfit = ACCOUNT.getTrades().get(0);
        Trade worstTradePercent = ACCOUNT.getTrades().get(0);
        int winningTrades = 0;
        int losingTrades = 0;

        for (Trade trade : ACCOUNT.getTrades()) {
            avgHoldingTime += trade.getHoldingTime();

            if (trade.getProfitPercent() > bestTradePercent.getProfitPercent()) bestTradePercent = trade;
            if (trade.getProfitPercent() < worstTradePercent.getProfitPercent()) worstTradePercent = trade;
            if (trade.getProfit() > bestTradeProfit.getProfit()) bestTradeProfit = trade;
            if (trade.getProfit() < worstTradeProfit.getProfit()) worstTradeProfit = trade;

            if (trade.getProfitPercent() > 0.0) winningTrades++;
            else losingTrades++;
        }

        String holdingTime = String.format("[!] Avg holding time: %s",
                Utils.msToNice(avgHoldingTime / ACCOUNT.getTrades().size(), false));
        LOGGER.info(holdingTime);
        StratX.trace(holdingTime);
        LOGGER.info("[!] Winning trades: {} ({})", MathUtils.COMMAS.format(winningTrades), MathUtils.getPercent(winningTrades, ACCOUNT.getTrades().size()));
        LOGGER.info("[!] Losing trades: {} ({})\n", MathUtils.COMMAS.format(losingTrades), MathUtils.getPercent(losingTrades, ACCOUNT.getTrades().size()));
        LOGGER.info("-- Best trade --");
        LOGGER.info("By $: " + bestTradeProfit);
        LOGGER.info("By %: " + bestTradePercent);
        LOGGER.info("-- Worst trade --");
        LOGGER.info("By $: " + worstTradeProfit);
        LOGGER.info("By %: " + worstTradePercent);
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public Configuration getConfig() {
        return CONFIG;
    }

    public boolean shouldShowSignals() {
        return SHOW_SIGNALS && SHOW_GUI && GUI != null;
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
        return PRICE_DATA.substring(PRICE_DATA.lastIndexOf('\\') + 1).split("_")[0];
    }

    public void addLock(XYSeries series) {
        series.setNotify(false);
        INDICATOR_LOCKS.add(series);
    }

    public double getCurrentPrice() {
        return currentCandle.getClose();
    }
}
