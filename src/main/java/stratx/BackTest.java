package stratx;

import stratx.gui.BacktestGUI;
import stratx.indicators.Indicator;
import stratx.indicators.Test;
import stratx.utils.Candlestick;
import stratx.utils.MathUtils;
import stratx.utils.Signal;
import stratx.utils.Trade;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class BackTest {
    // Config
    private final String PRICE_DATA = "src/main/resources/ETH-USD_15m_2022-03-13.json";
    private final Candlestick.Interval INTERVAL = Candlestick.Interval.FIFTEEN_MINUTES;
    private final boolean SHOW_GUI = true;
    private final boolean AUTO_SCALE = true;
    private final int MAX_CANDLES = 200;

    // Trading Config
    private final double STARTING_BALANCE = 100;
    private final int MAX_OPEN_TRADES = 1;
    private final double TAKE_PROFIT = 5.65; // Percent
    private final boolean USE_STOP_LOSS = false;
    private final double STOP_LOSS = 0.50;
    private final boolean USE_TRAILING_STOP = true;
    private final double ARM_TRAILING_STOP_AT = 1.0; // Enable when we hit this % of profit
    private final double TRAILING_STOP = 0.35;
    private final boolean SELL_BASED_ON_INDICATORS = true; // Turn off to use stop loss/take profit only
    private final boolean CLOSE_OPEN_TRADES_ON_EXIT = true;

    private List<Candlestick> data;
    private BacktestGUI GUI;
    private final ArrayList<Indicator> indicators = new ArrayList<>();
    private Candlestick currentCandle;

    public static void main(String... args) {
        // Load the price data in
        BackTest test = new BackTest();
        test.loadData();

        // Enabled indicators
        //test.indicators.add(new RSI(14, 70, 30));
        //test.indicators.add(new EMA(20));
        //test.indicators.add(new SupportResistance(50.0D));
        test.indicators.add(new Test());
        //test.indicators.add(new DojiTest());

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

    private void runTest() {
        if (SHOW_GUI) {
            GUI = new BacktestGUI(PRICE_DATA, INTERVAL, 1800, 900);
            //gui.populate(data, AUTO_SCALE, MAX_CANDLES);
            GUI.show();
        }

        System.out.println();
        StratX.log("-- Begin --");

        int index = 0;
        int openTrades = 0;
        double balance = STARTING_BALANCE;
        final int requiredSignals = this.indicators.size();
        ArrayList<Trade> trades = new ArrayList<>();
        Candlestick last = null;
        for (Candlestick candle : data) {
            if (index > 400) break; // @todo temp
            currentCandle = candle;
            GUI.getChartRenderer().addCandle(candle);

            int buySignals = 0;
            int sellSignals = 0;
            for (Indicator indicator : indicators) {
                indicator.update(candle);

                Signal signal = indicator.getSignal();
                if (signal == Signal.BUY) buySignals++;
                if (signal == Signal.SELL) sellSignals++;
            }

            // Make sure if more indicators are saying sell, dont enter a buy trade
            if (buySignals >= requiredSignals && buySignals >= sellSignals && openTrades < MAX_OPEN_TRADES && balance > 0) {
                trades.add(new Trade(this, candle, balance / MAX_OPEN_TRADES));
                balance -= balance / MAX_OPEN_TRADES;
                openTrades++;
            } else if (sellSignals >= requiredSignals && SELL_BASED_ON_INDICATORS && openTrades > 0) {
                Trade closed = trades.get(trades.size() - 1);
                closed.close(candle, "Indicator Signal");
                balance += closed.getProfit() + closed.getEntryAmountUSD();
                openTrades--;
            }

            // Take profit & stop loss check
            if (openTrades > 0) {
                for (Trade trade : trades) {
                    if (!trade.isOpen()) continue;
                    double profitPercent = trade.getProfitPercent();
                    boolean takeProfit = profitPercent >= TAKE_PROFIT;
                    boolean stopLoss = USE_STOP_LOSS && profitPercent <= -STOP_LOSS;

                    if (takeProfit || stopLoss) {
                        trade.close(candle, takeProfit ? "Take Profit" : "Stop Loss");
                        openTrades--;
                        balance += trade.getProfit() + trade.getEntryAmountUSD();
                    } else if (USE_TRAILING_STOP) {
                        if (profitPercent >= ARM_TRAILING_STOP_AT) trade.setTrailingStopArmed(true);
                        if (trade.isTrailingStopArmed()) {
                            double profitDiff = profitPercent - trade.getLastProfitPercent();
                            if (profitDiff <= -TRAILING_STOP) trade.close(candle, "Trailing Stop");
                            trade.setLastProfitPercent(profitPercent);
                        }
                    }

                    trade.setLastProfitPercent(profitPercent);
                }
            }

            index++;
            last = candle;
        }

        StratX.log("-- End --");

        // Close any open trades
        if (CLOSE_OPEN_TRADES_ON_EXIT) {
            System.out.println();
            StratX.log("-- Closing any remaining open trades --");
            int closed = 0;

            for (Trade trade : trades) {
                if (!trade.isOpen()) continue;
                trade.close(last, "Closing on exit");
                openTrades--;
                balance += trade.getProfit() + trade.getEntryAmountUSD();
                closed++;
            }
            
            StratX.log("-- Closed %d open trades --\n", closed);
        }

        System.out.println("[!] Final Balance: $" + MathUtils.roundTwoDec(balance) + " USD "
                + (MathUtils.getPercent(balance - STARTING_BALANCE, STARTING_BALANCE))
                + " (" + trades.size() + " trade" + (trades.size() == 1 ? "" : "s") + " made)");

        int bestTrade = 0;
        int worstTrade = 0;
        for (int i = 0; i < trades.size(); i++) {
            Trade trade = trades.get(i);
            if (trade.getProfitPercent() > trades.get(bestTrade).getProfitPercent()) bestTrade = i;
            if (trade.getProfitPercent() < trades.get(worstTrade).getProfitPercent()) worstTrade = i;
        }
        System.out.print("Best trade: ");
        System.out.println(trades.get(bestTrade));
        System.out.print("Worst trade: ");
        System.out.println(trades.get(worstTrade));
    }

    public BacktestGUI getGUI() {
        return GUI;
    }

    public Candlestick getCurrentCandle() {
        return currentCandle;
    }
}
