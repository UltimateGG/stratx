package stratx;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.utils.MathUtils;
import stratx.utils.Utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class Downloader {
    private final Logger LOGGER = LogManager.getLogger("Downloader");
    private final BinanceApiRestClient CLIENT = BinanceApiClientFactory.newInstance().newRestClient();
    private final int MAX_CANDLES_PER_REQUEST = 1000; // Binance limitation
    private final String DATA_FOLDER = "src/main/resources/downloader/";
    private final int BREAK_SECONDS = 5;

    private boolean downloading = false;


    // @TODO Input from console or GUI
    public static void main(String... args) {
        Downloader downloader = new Downloader();

        String symbol = "RVNUSDT";
        long daysHist = 365;
        long startTime = System.currentTimeMillis() - (1000 * 60 * 60 * 24 * daysHist);
        long endTime = System.currentTimeMillis();
        CandlestickInterval interval = CandlestickInterval.FIFTEEN_MINUTES;

        try {
            downloader.download(symbol, startTime, endTime, interval);
        } catch (Exception e) {
            StratX.error("Caught exception during download: ", e);
            System.exit(1);
        }
    }

    private String createFile(String symbol, CandlestickInterval interval, long startTime) throws IOException {
        File dataFolder = new File(DATA_FOLDER);

        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                LOGGER.error("Failed to create data folder: " + dataFolder.getAbsolutePath());
                throw new IOException("Failed to create data folder: " + dataFolder.getAbsolutePath());
            }
        }

        String fileName = String.format("%s%s_%s_%s", DATA_FOLDER, symbol, interval.getIntervalId(), formatDate(new Date(startTime)));
        if (new File(fileName + ".strx").exists()) fileName += "_" + Utils.getRandomString(5);
        fileName += ".strx";
        File dataFile = new File(fileName);

        if (dataFolder.exists() && dataFolder.isDirectory() && !dataFile.exists() && dataFile.createNewFile()) {
            LOGGER.info("Created data file: " + dataFile.getAbsolutePath());
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
            LOGGER.error("Invalid symbol: " + symbol);
            System.exit(1);
        }

        LOGGER.info("Downloading {} on {} interval ({} candlesticks/{} requests)..", symbol, interval.getIntervalId(), MathUtils.COMMAS.format(totalCandles), numRequests);

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
                        LOGGER.warn("Duplicate candle time: " + candle.getCloseTime());
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
                    LOGGER.info("Sent {}/{} requests, {} candles", i, numRequests, MathUtils.COMMAS.format((long) i * MAX_CANDLES_PER_REQUEST));
                    System.gc();
                }

                if (i % 15 == 0 && i > 0) {
                    LOGGER.info("Sleeping for {} seconds", BREAK_SECONDS);
                    try {
                        Thread.sleep(BREAK_SECONDS * 60 * 1000L);
                    } catch (Exception ignored) {}
                }
            }

            output.flush();
        }

        downloading = false;
        LOGGER.info("Download complete!");
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

    public boolean isDownloading() {
        return downloading;
    }
}
