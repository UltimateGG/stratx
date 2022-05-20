package stratx.modes;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.domain.event.CandlestickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.Loader;
import stratx.StratX;
import stratx.gui.Gui;
import stratx.strategies.Strategy;
import stratx.utils.*;

import java.awt.*;
import java.io.Closeable;
import java.net.SocketTimeoutException;
import java.util.List;

/** Shell/base class for different modes. Live & Simulation mode need
 * a market data stream of candlesticks. Backtest simulates this stream.
 * Downloader just leaves those methods blank. */
public abstract class Mode {
    protected final Type TYPE;
    protected String COIN;
    protected final boolean SHOW_GUI;
    protected Gui GUI;
    protected final Logger LOGGER;
    protected final double STARTING_BALANCE;
    protected Closeable candlestickEventListener;
    protected PriceHistory priceHistory;
    protected Candlestick currentCandle = null;
    protected Candlestick previousCandle = null;
    protected double lastPrice = 0.0;
    protected final Strategy strategy;
    protected final Account ACCOUNT;
    protected boolean isConnectedToMarket = false;


    public Mode(Type type, Strategy strategy, String coin) {
        this.TYPE = type;
        this.strategy = strategy;
        this.COIN = coin;
        this.SHOW_GUI = !GraphicsEnvironment.isHeadless() && StratX.getConfig().getBoolean(type.getConfigKey() + ".show-gui", true);
        this.LOGGER = LogManager.getLogger(type.toString());

        this.STARTING_BALANCE = TYPE == Type.LIVE ?
                Double.parseDouble(StratX.API.get().getAccount().getAssetBalance(StratX.getTradingAsset()).getFree()) :
                StratX.getConfig().getDouble((TYPE == Type.SIMULATION ? "simulation" : "backtest") + ".starting-balance", 100.0);
        if (STARTING_BALANCE <= strategy.MIN_USD_PER_TRADE || STARTING_BALANCE <= 0.0)
            throw new IllegalStateException("Starting balance must be greater than MIN_USD_PER_TRADE and 0.0");

        double fee = StratX.getConfig().getDouble("buy-sell-fee-percent", 0.1) / 100.0;
        this.ACCOUNT = new Account(STARTING_BALANCE, type == Type.LIVE ? 0.0 : fee);
    }

    public void begin() {
        if (TYPE.requiresMarketDataStream()) setupReconnect();
        ACCOUNT.reset();

        if (this.strategy != null) {
            LOGGER.info("Using strategy: " + strategy.name);
            StratX.trace(this.strategy.toString());
        }

        this.start();
    }

    private void setupReconnect() {
        try {
            setupMarketDataStream();
        } catch (Exception e) {
            LOGGER.error("Error during market streaming", e);
            LOGGER.error("Reconnecting in 30 seconds...");
            isConnectedToMarket = false;

            try {
                Thread.sleep(30_000);
                candlestickEventListener.close();
            } catch (Exception e1) {
                LOGGER.error("Error closing data stream", e1);
            } finally {
                setupReconnect();
            }
        }
    }

    private void setupMarketDataStream() {
        priceHistory = new PriceHistory(200);
        final double[] prevPrice = {0};

        LOGGER.info("(Market stream) Trading on " + strategy.CANDLESTICK_INTERVAL + " interval");
        StratX.trace("Trading on " + strategy.CANDLESTICK_INTERVAL + " interval");
        candlestickEventListener = StratX.API.getWebsocket().onCandlestickEvent(COIN, strategy.CANDLESTICK_INTERVAL, new BinanceApiCallback<CandlestickEvent>() {
            @Override
            public void onResponse(CandlestickEvent event) {
                isConnectedToMarket = true;
                lastPrice = Double.parseDouble(event.getClose());
                Candlestick candle = new Candlestick(
                        event.getCloseTime(),
                        Double.parseDouble(event.getOpen()),
                        Double.parseDouble(event.getHigh()),
                        Double.parseDouble(event.getLow()),
                        Double.parseDouble(event.getClose()),
                        (long) Double.parseDouble(event.getVolume()),
                        previousCandle, false
                );

                candle.setFinal(event.getBarFinal());
                currentCandle = candle;

                if (candle.isFinal()) {
                    onCandleClose(candle);
                    priceHistory.add(candle);
                    previousCandle = candle;
                    return;
                }

                // A candle updated, but didn't close, fire events for stop loss/take profit
                onPriceUpdate(prevPrice[0], Double.parseDouble(event.getClose()));
                prevPrice[0] = Double.parseDouble(event.getClose());
            }

            @Override
            public void onFailure(Throwable cause) {
                LOGGER.error("Error during candlestick stream", cause);
                isConnectedToMarket = false;

                if (cause instanceof SocketTimeoutException) {
                    LOGGER.error("Socket timeout (internet down?) reconnecting in 30 seconds...");

                    try {
                        Thread.sleep(30_000);
                        candlestickEventListener.close();
                    } catch (Exception e) {
                        LOGGER.error("Error closing data stream", e);
                    } finally {
                        setupReconnect();
                    }
                }
            }
        });

        // Populate price history
        List<com.binance.api.client.domain.market.Candlestick> bars = StratX.API.get().getCandlestickBars(COIN, strategy.CANDLESTICK_INTERVAL);

        for (com.binance.api.client.domain.market.Candlestick bar : bars) {
            Candlestick candle = new Candlestick(
                    bar.getCloseTime(),
                    Double.parseDouble(bar.getOpen()),
                    Double.parseDouble(bar.getHigh()),
                    Double.parseDouble(bar.getLow()),
                    Double.parseDouble(bar.getClose()),
                    (long) Double.parseDouble(bar.getVolume()),
                    previousCandle, true);

            priceHistory.add(candle);
            this.strategy.getIndicators().forEach(indicator -> {
                if (indicator.getPriceHistory() != null)
                    indicator.getPriceHistory().add(candle);
            });
            previousCandle = candle;
        }
        bars.clear();
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
        strategy.onCandleClose(candle);
        Signal signal = strategy.getSignal();

        if (signal == Signal.BUY) forceBuy();
        else if (signal == Signal.SELL) forceSell();
    }

    public void forceBuy() {
        double amt = strategy.getBuyAmount();
        if (amt > 0.0 && ACCOUNT.getBalance() >= amt && strategy.isValidBuy(amt))
            ACCOUNT.openTrade(new Trade(this, amt));
    }

    public void forceSell() {
        for (Trade trade : ACCOUNT.getTrades()) {
            if (!trade.isOpen() || !strategy.isValidSell()) continue;
            ACCOUNT.closeTrade(trade, "Indicator Signal");
            if (strategy.SELL_ALL_ON_SIGNAL) break;
        }
    }

    /** Close any open trades */
    protected void closeOpenTrades() {
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
        return StratX.getConfig().getBoolean("show-buy-sell-signals", true) && SHOW_GUI && GUI != null;
    }

    public Gui getGUI() {
        return GUI;
    }

    public Logger getLogger() {
        return LOGGER;
    }

    public Candlestick getCurrentCandle() {
        return currentCandle;
    }

    public Candlestick getPreviousCandle() {
        return previousCandle;
    }

    public double getCurrentPrice() {
        return lastPrice;
    }

    public long getCurrentTime() {
        if (currentCandle == null) return 0;
        return currentCandle.getCloseTime();
    }

    public Account getAccount() {
        return ACCOUNT;
    }

    public boolean isConnectedToMarket() {
        return isConnectedToMarket;
    }

    protected void printResults() {
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

        if (getType() == Type.BACKTEST) LOGGER.info("[!] " + Loader.lastDataRange);
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

    public static enum Type {
        BACKTEST(false, "backtest"),
        DOWNLOAD(false, "downloader"),
        SIMULATION(true, "simulation"),
        LIVE(true, "live-trading");

        private final boolean requiresMarketDataStream;
        private final String configKey;

        Type(boolean requiresMarketDataStream, String configKey) {
            this.requiresMarketDataStream = requiresMarketDataStream;
            this.configKey = configKey;
        }

        public boolean requiresMarketDataStream() {
            return requiresMarketDataStream;
        }

        public String getConfigKey() {
            return configKey;
        }
    }
}
