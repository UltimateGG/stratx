package stratx.modes;

import stratx.Loader;
import stratx.StratX;
import stratx.gui.Gui;
import stratx.gui.GuiTheme;
import stratx.strategies.Strategy;
import stratx.utils.Candlestick;
import stratx.utils.CurrencyPair;
import stratx.utils.MathUtils;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BackTest extends Mode {
    private String PRICE_DATA;
    private List<Candlestick> data;


    public BackTest(Strategy strategy) {
        super(Type.BACKTEST, strategy, null);
        showFilePickerGui();
    }

    private void setup(String priceDataFile) {
        this.PRICE_DATA = priceDataFile;
        this.setCoin(new CurrencyPair(PRICE_DATA.substring(PRICE_DATA.lastIndexOf('\\') + 1).split("_")[0]));
        this.loadData(this.PRICE_DATA); // Load the price data in
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

    @Override
    protected void start() {
        if (SHOW_GUI) {
            GUI = new Gui("StratX Backtest", 1800, 900, false);
            GUI.setIcon("/icon.png");
            GUI.addCandlestickChart(this.getCoin().toString());
        }

        StratX.trace(PRICE_DATA.substring(PRICE_DATA.lastIndexOf('\\') + 1));
        StratX.trace("Starting backtest at {} ({}) on {} candles...", new Date(), System.currentTimeMillis(), data.size());
        StratX.trace("Starting balance: ${}", MathUtils.COMMAS.format(STARTING_BALANCE));

        LOGGER.info("Running test with a starting balance of ${}\n\n", MathUtils.COMMAS.format(STARTING_BALANCE));
        LOGGER.info("-- Begin --");

        for (Candlestick candle : data) {
            this.currentCandle = candle;
            lastPrice = candle.getUnmodifiedClose();
            this.onPriceUpdate(previousCandle == null ? candle.getClose() : previousCandle.getClose(), candle.getClose());
            this.onCandleClose(candle);
            this.previousCandle = candle;
        }

        LOGGER.info("-- End --");
        if (strategy.CLOSE_OPEN_TRADES_ON_EXIT) this.closeOpenTrades();
        this.printResults();

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

    private void showFilePickerGui() {
        Gui gui = new Gui("StratX Backtest", 350, 195, false);
        ArrayList<File> files = getValidFiles();
        String[] fileNames = new String[files.size()];

        for (int i = 0; i < files.size(); i++) fileNames[i] = files.get(i).getName().replace(".strx", "");

        gui.addPanel();
        JComboBox<?> fileSelect = gui.addDropdown("Select Backtest File", new JComboBox<>(fileNames), 0);
        gui.addLabel("Strategy: " + this.strategy.name, 10);
        gui.addPaddingY(10);
        gui.addButton("Run", GuiTheme.INFO_COLOR, GuiTheme.TEXT_COLOR, null, 10).addActionListener(e -> {
            File file = files.get(fileSelect.getSelectedIndex());
            if (file != null) {
                setup(file.getAbsolutePath());
                gui.close();
                this.begin();
            }
        });

        gui.show();
    }

    private ArrayList<File> getValidFiles() {
        File[] files = new File(StratX.DATA_FOLDER + "\\downloader\\").listFiles();

        if (files == null || files.length == 0) {
            StratX.getLogger().error("No files found in {}, download them using the downloader mode!", StratX.DATA_FOLDER + "\\downloader\\");
            System.exit(1);
        }

        List<File> validFiles = new ArrayList<>();
        for (File file : files)
            if (file.getName().endsWith(".strx")) validFiles.add(file);

        return new ArrayList<>(validFiles);
    }
}
