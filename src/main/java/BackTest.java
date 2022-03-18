import org.jfree.data.time.FixedMillisecond;
import stratx.Loader;
import stratx.StratX;
import stratx.gui.BacktestGUI;
import stratx.indicators.EMA;
import stratx.indicators.Indicator;
import stratx.indicators.RSI;
import stratx.indicators.SupportResistance;
import stratx.utils.Candlestick;
import stratx.utils.Signal;

import java.util.ArrayList;
import java.util.List;

public class BackTest {
    // Config
    private static final String PRICE_DATA = "src/main/resources/BTC-USD_15m.json";
    private static final Candlestick.Interval INTERVAL = Candlestick.Interval.FIFTEEN_MINUTES;
    private static final boolean SHOW_GUI = true;
    private static final boolean AUTO_SCALE = true;
    private static final int MAX_CANDLES = 200;

    private List<Candlestick> data;
    private BacktestGUI gui;
    private final ArrayList<Indicator> indicators = new ArrayList<>();

    public static void main(String... args) {
        // Load the price data in
        BackTest test = new BackTest();
        test.loadData();

        // Enabled indicators
        //test.indicators.add(new RSI(14, 70, 30));
        //test.indicators.add(new EMA(25));
        test.indicators.add(new SupportResistance(50.0D));

        StratX.log("Running backtest...");
        test.runTest();
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

    /**
     * Essentially, we need to simulate the market moving forward
     * one candlestick at a time by looping through the array.
     *
     *  It cannot see the future, so it will only see the current
     *  and previous candlesticks.
     *
     *
     */
    private void runTest() {
        // Run gui on separate thread
        if (SHOW_GUI) {
            gui = new BacktestGUI(PRICE_DATA, INTERVAL, 1800, 900);
            //gui.populate(data, AUTO_SCALE, MAX_CANDLES);
            gui.show();
        }

        int index = 0;
        int openTrades = 0;
        Candlestick last = null;
        for (Candlestick cd : data) {
            if (index > 150) break; // @todo temp
            Candlestick candle = cd.toHeikinAshi(last);
            gui.getChartRenderer().addCandle(candle);

            int buySignals = 0;
            int sellSignals = 0;
            for (Indicator indicator : indicators) {
                indicator.update(candle);

                Signal signal = indicator.getSignal();
                if (signal == Signal.BUY) buySignals++;
                if (signal == Signal.SELL) sellSignals++;
            }

            if (buySignals == 2) {
                gui.getChartRenderer().addSignalIndicatorOn(candle.getID(), Signal.BUY);
                openTrades++;
            }
            if (sellSignals == 2 && openTrades > 0) {
                gui.getChartRenderer().addSignalIndicatorOn(candle.getID(), Signal.SELL);
                openTrades--;
            }

            index++;
            last = cd;
            try {
                Thread.sleep(3);
            } catch (Exception ignored) {}
        }
    }
}
