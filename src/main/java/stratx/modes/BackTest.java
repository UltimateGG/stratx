package stratx.modes;

import stratx.Loader;
import stratx.StratX;
import stratx.gui.GuiFactory;
import stratx.utils.Candlestick;
import stratx.utils.MathUtils;
import stratx.utils.Trade;
import stratx.utils.Utils;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

@SuppressWarnings("FieldCanBeLocal")
public class BackTest extends Mode {
    private final String PRICE_DATA;
    private final boolean SHOW_SIGNALS = CONFIG.getBoolean("backtest.show-signals", true);
    private List<Candlestick> data;


    public BackTest(boolean showGui) {
        super(Type.BACKTEST, null, showGui);
        this.PRICE_DATA = getFileFromUser().getAbsolutePath();

        this.setCoin(PRICE_DATA.substring(PRICE_DATA.lastIndexOf('\\') + 1).split("_")[0]);
        this.loadData(this.PRICE_DATA); // Load the price data in
    }

    @Override
    public void start() {
        try {
            this.runTest();
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

    private void runTest() {
        if (SHOW_GUI) {
            GUI = GuiFactory.createStaticGui("StratX Backtest", 1800, 900);
            GUI.setIcon("/icon.png");
            GUI.addCandlestickChart(this.getCoin());
        }

        StratX.trace(PRICE_DATA.substring(PRICE_DATA.lastIndexOf('\\') + 1));
        StratX.trace("Starting backtest at {} ({}) on {} candles...", new Date(), System.currentTimeMillis(), data.size());
        StratX.trace("Starting balance: ${}", MathUtils.COMMAS.format(STARTING_BALANCE));
        StratX.trace(strategy.toString());

        LOGGER.info("Running test with a starting balance of ${}\n\n", MathUtils.COMMAS.format(STARTING_BALANCE));
        LOGGER.info("-- Begin --");

        for (Candlestick candle : data) {
            this.currentCandle = candle;
            this.onPriceUpdate(previousCandle == null ? candle.getClose() : previousCandle.getClose(), candle.getClose());
            this.onCandleClose(candle);
            this.previousCandle = candle;
        }

        LOGGER.info("-- End --");
        if (strategy.CLOSE_OPEN_TRADES_ON_EXIT)
            this.closeOpenTrades();

        printResults();

        // Only update after for performance
        if (SHOW_GUI) {
            GUI.getCandlestickChart().update();
            GUI.show();
        }
    }

    @Override
    protected void onPriceUpdate(double prevPrice, double newPrice) {
        this.checkTakeProfitStopLoss();
    }

    @Override
    protected void onCandleClose(Candlestick candle) {
        if (SHOW_GUI) GUI.getCandlestickChart().addCandle(candle);
        this.checkTakeProfitStopLoss();
        this.checkBuySellSignals(candle);
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

    private void printResults() {
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

    private static File getFileFromUser() {
        File[] files = new File(StratX.DATA_FOLDER + "\\downloader\\").listFiles();

        if (files == null || files.length == 0) {
            StratX.getLogger().error("No files found in {}, download them using the downloader mode!", StratX.DATA_FOLDER + "\\downloader\\");
            System.exit(1);
        }

        StratX.log("Found {} files. Please select a file to test on:", files.length);
        for (int i = 1; i < files.length; i++) StratX.log("[{}] {}", i, files[i].getName());

        StratX.log("Number: "); // @TODO config for max open trade time?, market data stream
        Scanner scanner = new Scanner(System.in);
        int fileIndex = scanner.nextInt();

        if (fileIndex < 1 || fileIndex > files.length || !files[fileIndex].getName().endsWith(".strx")) {
            StratX.getLogger().error("Invalid file index {}", fileIndex);
            System.exit(1);
        }

        File file = files[fileIndex];
        StratX.log("Selected {}", file.getName());

        return file;
    }
}
