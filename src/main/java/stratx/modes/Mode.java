package stratx.modes;

import com.binance.api.client.domain.market.CandlestickInterval;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.StratX;
import stratx.gui.Gui;
import stratx.strategies.Strategy;
import stratx.utils.*;

import java.io.Closeable;

/** Shell/base class for different modes. Live & Simulation mode need
 * a market data stream of candlesticks. Backtest simulates this stream.
 * Downloader just leaves those methods blank. */
public abstract class Mode {
    protected final Type TYPE;
    protected String COIN;
    protected final boolean SHOW_GUI;
    protected Gui GUI;
    protected final Logger LOGGER;
    protected final Configuration CONFIG = new Configuration("\\config\\config.yml");
    protected final double STARTING_BALANCE = CONFIG.getDouble("backtest.starting-balance", 100.0);
    protected Closeable candlestickEventListener;
    protected PriceHistory priceHistory;
    protected Candlestick currentCandle = null;
    protected Candlestick previousCandle = null;
    protected Strategy strategy;
    protected final Account ACCOUNT = new Account(STARTING_BALANCE);


    public Mode(Type type, String coin, boolean showGui) {
        this.TYPE = type;
        this.COIN = coin;
        this.SHOW_GUI = showGui;
        this.LOGGER = LogManager.getLogger(type.toString());
    }

    public void begin(Strategy strategy) {
        this.strategy = strategy;
        if (TYPE.requiresMarketDataStream()) setupMarketDataStream();
        ACCOUNT.reset();
        this.start();
    }

    private void setupMarketDataStream() {
        priceHistory = new PriceHistory(200); // @TODO make configurable?

        candlestickEventListener = StratX.API.getWebsocket().onCandlestickEvent(COIN, CandlestickInterval.FIFTEEN_MINUTES, event -> {
            if (event.getBarFinal() || previousCandle == null || event.getCloseTime() > previousCandle.getCloseTime()) {
                Candlestick candle = new Candlestick(
                    event.getCloseTime(),
                    Double.parseDouble(event.getOpen()),
                    Double.parseDouble(event.getHigh()),
                    Double.parseDouble(event.getLow()),
                    Double.parseDouble(event.getClose()),
                    (long) Double.parseDouble(event.getVolume()),
                    previousCandle
                );

                candle.setFinal(event.getBarFinal() || (previousCandle != null && candle.getCloseTime() > previousCandle.getCloseTime()));
                currentCandle = candle;
                priceHistory.add(candle);
                if (candle.isFinal()) this.onCandleClose(candle);
                previousCandle = candle;
            } else {
                Candlestick candle = priceHistory.getByTime(event.getCloseTime());

                // A candle updated, but didn't close, fire events for stop loss/take profit
                if (candle != null) {
                    currentCandle = candle;
                    this.onPriceUpdate(candle.getClose(), Double.parseDouble(event.getClose()));

                    candle.setHigh(Double.parseDouble(event.getHigh()));
                    candle.setLow(Double.parseDouble(event.getLow()));
                    candle.setClose(Double.parseDouble(event.getClose()));
                    candle.setVolume((long) Double.parseDouble(event.getVolume()));
                }
            }
        });
    }

    /** Called to begin running the mode */
    protected abstract void start();

    /** Called every few seconds to show price updates */
    protected abstract void onPriceUpdate(double prevPrice, double newPrice);

    /** Called when a candlestick is closed/now immutable. */
    protected abstract void onCandleClose(Candlestick candle);

    /** Take profit, stop loss, & trailing stop loss check */
    protected void checkTakeProfitStopLoss() {
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

    /** Places buy/sell orders */
    protected void checkBuySellSignals(Candlestick candle) {
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

    public Type getType() {
        return TYPE;
    }

    public String getCoin() {
        return COIN;
    }

    public void setCoin(String coin) {
        COIN = coin;
    }

    public boolean isShowGUI() {
        return SHOW_GUI && GUI != null;
    }

    public boolean shouldShowSignals() {
        return CONFIG.getBoolean("show-buy-sell-signals", true) && SHOW_GUI && GUI != null;
    }

    public Gui getGUI() {
        return GUI;
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public Configuration getConfig() {
        return CONFIG;
    }

    public Candlestick getCurrentCandle() {
        return currentCandle;
    }

    public Candlestick getPreviousCandle() {
        return previousCandle;
    }

    public double getCurrentPrice() {
        if (currentCandle == null) return 0;
        return currentCandle.getClose();
    }

    public Account getAccount() {
        return ACCOUNT;
    }

    public static enum Type {
        BACKTEST(false),
        DOWNLOAD(false),
        SIMULATION(true),
        LIVE(true);

        private final boolean requiresMarketDataStream;

        Type(boolean requiresMarketDataStream) {
            this.requiresMarketDataStream = requiresMarketDataStream;
        }

        public boolean requiresMarketDataStream() {
            return requiresMarketDataStream;
        }
    }
}
