package stratx.modes;

import com.binance.api.client.BinanceApiCallback;
import com.binance.api.client.domain.event.CandlestickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.StratX;
import stratx.gui.Gui;
import stratx.strategies.Strategy;
import stratx.utils.*;

import java.awt.*;
import java.io.Closeable;
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
    protected final Strategy strategy;
    protected final Account ACCOUNT;


    public Mode(Type type, Strategy strategy, String coin) {
        this.TYPE = type;
        this.strategy = strategy;
        this.COIN = coin;
        this.SHOW_GUI = !GraphicsEnvironment.isHeadless() && StratX.getConfig().getBoolean(type.getConfigKey() + ".show-gui", true);
        this.LOGGER = LogManager.getLogger(type.toString());

        // @TODO If live, use balance at instantiation.
        this.STARTING_BALANCE = StratX.getConfig().getDouble((TYPE == Type.SIMULATION ? "simulation" : "backtest") + ".starting-balance", 100.0);
        this.ACCOUNT = new Account(STARTING_BALANCE);
    }

    public void begin() {
        if (TYPE.requiresMarketDataStream()) setupMarketDataStream();
        ACCOUNT.reset();

        if (this.strategy != null) {
            LOGGER.info("Using strategy: " + strategy.name);
            StratX.trace(this.strategy.toString());
        }

        this.start();
    }

    private void setupMarketDataStream() {
        priceHistory = new PriceHistory(200);

        LOGGER.info("Trading on " + strategy.CANDLESTICK_INTERVAL + " interval");
        StratX.trace("Trading on " + strategy.CANDLESTICK_INTERVAL + " interval");
        candlestickEventListener = StratX.API.getWebsocket().onCandlestickEvent(COIN, strategy.CANDLESTICK_INTERVAL, new BinanceApiCallback<CandlestickEvent>() {
            @Override
            public void onResponse(CandlestickEvent event) {
                Candlestick fromHistory = priceHistory.getByTime(event.getCloseTime());
                Candlestick candle = fromHistory != null ? fromHistory : new Candlestick(
                        event.getCloseTime(),
                        Double.parseDouble(event.getOpen()),
                        Double.parseDouble(event.getHigh()),
                        Double.parseDouble(event.getLow()),
                        Double.parseDouble(event.getClose()),
                        (long) Double.parseDouble(event.getVolume()),
                        previousCandle, false
                );

                if (candle.isFinal()) return;
                candle.setFinal(event.getBarFinal());
                currentCandle = candle;
                if (!priceHistory.get().contains(candle)) priceHistory.add(candle);

                if (candle.isFinal()) {
                    onCandleClose(candle);
                    previousCandle = candle;
                    return;
                }

                // A candle updated, but didn't close, fire events for stop loss/take profit
                onPriceUpdate(candle.getClose(), Double.parseDouble(event.getClose()));

                candle.setHigh(Double.parseDouble(event.getHigh()));
                candle.setLow(Double.parseDouble(event.getLow()));
                candle.setClose(Double.parseDouble(event.getClose()));
                candle.setVolume((long) Double.parseDouble(event.getVolume()));
            }

            @Override
            public void onFailure(Throwable cause) {
                LOGGER.error("Error during candlestick stream", cause);
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
        if (currentCandle == null) return 0;
        return currentCandle.getClose();
    }

    public long getCurrentTime() {
        if (currentCandle == null) return 0;
        return currentCandle.getCloseTime();
    }

    public Account getAccount() {
        return ACCOUNT;
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
