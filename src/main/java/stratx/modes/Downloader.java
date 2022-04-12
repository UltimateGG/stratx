package stratx.modes;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.github.lgooddatepicker.components.DatePicker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.StratX;
import stratx.gui.Gui;
import stratx.gui.GuiTheme;
import stratx.utils.Configuration;
import stratx.utils.MathUtils;
import stratx.utils.Utils;

import javax.swing.*;
import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class Downloader {
    private final BinanceApiRestClient CLIENT = BinanceApiClientFactory.newInstance().newRestClient(); // No credentials needed
    private final int MAX_CANDLES_PER_REQUEST = 1000; // Binance limitation
    private final String DATA_FOLDER = StratX.DATA_FOLDER + "downloader\\";
    private final Configuration CONFIG = new Configuration("\\config\\config.yml");
    private final int BREAK_SECONDS = CONFIG.getInt("downloader.break-seconds", 3);
    private final Logger LOGGER = LogManager.getLogger("DOWNLOADER");

    private boolean downloading = false;
    private JButton downloadButton;
    private JTextArea console;


    public void run() {
        Gui newGui = new Gui("StratX Downloader", 450, 600, false);
        newGui.setCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        newGui.addPanel();

        JTextField symbolField = newGui.addTextbox("Symbol", "BTCUSDT");
        JComboBox<?> intervalBox = newGui.addDropdown("Interval", new JComboBox<>(CandlestickInterval.values()), 2);
        DatePicker startDatePicker = newGui.addDatePicker(null, "Start Date");
        DatePicker endDatePicker = newGui.addDatePicker(null, "End Date");

        startDatePicker.setDate(startDatePicker.getDate().minusDays(31)); // Default to 31 days back
        newGui.addPaddingY(20);

        downloadButton = newGui.addButton("Download", GuiTheme.INFO_COLOR, GuiTheme.TEXT_COLOR, null, 10);
        newGui.addPaddingY(10);
        console = newGui.addConsoleLog(150);

        downloadButton.addActionListener(e -> {
            console.setText("");
            downloadButton.setEnabled(false);

            String symbol = symbolField.getText();
            long startTime = localDateToEpoch(startDatePicker.getDate());
            long endTime = localDateToEpoch(endDatePicker.getDate());
            CandlestickInterval interval = (CandlestickInterval) intervalBox.getSelectedItem();

            new Thread(() -> {
                try {
                    download(symbol, startTime, endTime, interval);
                } catch (Exception ex) {
                    log("ERROR: Caught exception during download: %s", ex);
                    LOGGER.error("ERROR: Caught exception during download: ", ex);
                    downloading = false;
                    downloadButton.setEnabled(true);
                }
            }).start();
        });

        newGui.show();
        log("Console: Downloader initialized");
    }

    private String createFile(String symbol, CandlestickInterval interval, long startTime) throws IOException {
        File dataFolder = new File(DATA_FOLDER);

        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                log("ERROR: Failed to create data folder: " + dataFolder.getAbsolutePath());
                throw new IOException("Failed to create data folder: " + dataFolder.getAbsolutePath());
            }
        }

        String fileName = String.format("%s%s_%s_%s", DATA_FOLDER, symbol, interval.getIntervalId(), formatDate(new Date(startTime)));
        if (new File(fileName + ".strx").exists()) fileName += "_" + Utils.getRandomString(5);
        fileName += ".strx";
        File dataFile = new File(fileName);

        if (dataFolder.exists() && dataFolder.isDirectory() && !dataFile.exists() && dataFile.createNewFile()) {
            log("Created data file: " + dataFile.getAbsolutePath());
            return dataFile.getAbsolutePath();
        }

        throw new IOException("Failed to create data file: " + dataFile.getAbsolutePath());
    }

    public void download(String symbol, final long startTime, final long endTime, CandlestickInterval interval) throws IOException {
        if (downloading) throw new IllegalStateException("Already downloading");
        String fileName = createFile(symbol, interval, startTime);
        downloading = true;

        int totalCandles = (int) ((endTime - startTime) / Utils.binanceIntervalToMs(interval));
        int numRequests = MathUtils.clampInt(totalCandles / MAX_CANDLES_PER_REQUEST, 1, Integer.MAX_VALUE);

        if (!isValidSymbol(symbol)) {
            log("ERROR: Invalid symbol: " + symbol);
            throw new IllegalArgumentException("Invalid symbol: " + symbol);
        }

        log("Downloading %s on %s interval (%s candlesticks/%s requests)..", symbol, interval.getIntervalId(), MathUtils.COMMAS.format(totalCandles), numRequests);

        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
            long currentStartTime = startTime;

            // the magic string is 'b4 ff b4 ff' + the version number
            output.writeByte(0xb4);
            output.writeByte(0xff);
            output.writeByte(0xb4);
            output.writeByte(0xff);
            output.writeByte(0x01);

            output.writeLong(startTime);
            output.writeLong(endTime);

            List<Long> times = new ArrayList<>();
            for (int i = 0; i < numRequests; i++) {
                List<Candlestick> downloaded = CLIENT.getCandlestickBars(symbol, interval, MAX_CANDLES_PER_REQUEST, currentStartTime, endTime);

                // Date Open High Low Close Volume
                for (Candlestick candle : downloaded) {
                    if (times.contains(candle.getCloseTime())) {
                        log("WARN: Duplicate candle time: " + candle.getCloseTime());
                        continue;
                    }

                    times.add(candle.getCloseTime());
                    output.writeLong(candle.getCloseTime());
                    output.writeDouble(Double.parseDouble(candle.getOpen()));
                    output.writeDouble(Double.parseDouble(candle.getHigh()));
                    output.writeDouble(Double.parseDouble(candle.getLow()));
                    output.writeDouble(Double.parseDouble(candle.getClose()));
                    output.writeLong((long) Double.parseDouble(candle.getVolume()));
                }

                downloaded.clear();
                output.flush();
                currentStartTime += Utils.binanceIntervalToMs(interval) * MAX_CANDLES_PER_REQUEST;
                if (i % 5 == 0 && i > 0) {
                    log("Sent %d/%d requests, %s candles", i, numRequests, MathUtils.COMMAS.format((long) i * MAX_CANDLES_PER_REQUEST));
                    System.gc();
                }

                if (i % 15 == 0 && i > 0) {
                    log("Sleeping for %d seconds", BREAK_SECONDS);
                    try {
                        Thread.sleep(BREAK_SECONDS * 1000L);
                    } catch (Exception ignored) {}
                }
            }

            output.flush();
        }

        downloading = false;
        if (downloadButton != null) downloadButton.setEnabled(true);
        log("Download complete!");
    }

    private void log(String msg, Object... args) {
        LOGGER.info(String.format(msg, args));
        if (console != null) {
            console.append(String.format(msg, args) + "\n");
            console.setCaretPosition(console.getDocument().getLength());
        }
    }

    private boolean isValidSymbol(String symbol) {
        try {
            return CLIENT.get24HrPriceStatistics(symbol).getAskPrice() != null;
        } catch (Exception ignored) {}
        return false;
    }

    private String formatDate(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return String.format("%s.%s.%s", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.YEAR));
    }

    private long localDateToEpoch(LocalDate d) {
        return d.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public boolean isDownloading() {
        return downloading;
    }
}
