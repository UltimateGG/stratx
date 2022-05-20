package stratx.strategies;

import com.binance.api.client.domain.market.CandlestickInterval;
import stratx.StratX;
import stratx.indicators.Indicator;
import stratx.utils.*;

import java.util.ArrayList;
import java.util.Arrays;

public class Strategy {
    /** The name of the strategy */
    public final String name;

    /** The max open trade positions at once */
    public int MAX_OPEN_TRADES = 1;

    /** Whether to use take profit or not */
    public boolean USE_TAKE_PROFIT = true;

    /** The percent of profit a position will sell at if greater than or equal to that percent [0-100]% */
    public double TAKE_PROFIT = 5.0;

    /** Whether to use the stop loss or not */
    public boolean USE_STOP_LOSS = true;

    /** The percent a position will sell at if the loss is greater than or equal to that percent [0-100]% */
    public double STOP_LOSS = 2.0;

    /** Whether to use a trailing stop loss or not */
    public boolean USE_TRAILING_STOP = false;

    /** The percent of profit to enable trailing stop at, once the profit
     * is greater than or equal to this amount, and then it drops by TRAILING_STOP percent,
     * the position will sell, locking in profits [0-100]% */
    public double ARM_TRAILING_STOP_AT = 0.1;

    /** The percent a position will sell at if the profit drops by this percent [0-100]% */
    public double TRAILING_STOP = 0.5;

    /** Whether to use indicator sell signals or not, if false
     * no positions will be sold unless by take profit, stop loss,
     * or trailing stop */
    public boolean SELL_BASED_ON_INDICATORS = true;

    /** Whether to close any remaining open positions before exiting the backtest/simulation or not */
    public boolean CLOSE_OPEN_TRADES_ON_EXIT = true;

    /** The minimum amount of indicators signaling BUY to open a position
     * If -1, this will use the amount of enabled indicators */
    public int MIN_BUY_SIGNALS = -1;

    /** The minimum amount of indicators signaling SELL to close a position
     * If -1, this will use the amount of enabled indicators */
    public int MIN_SELL_SIGNALS = -1;

    /** The maximum amount of USD a trade can be placed for
     * If -1, this will use all available money in the account
     * at that time */
    public double MAX_USD_PER_TRADE = -1;

    /** The minimum about of USD a trade can be placed for
     * If your balance is less than this, no trade is placed */
    public double MIN_USD_PER_TRADE = 1.0;

    /** The percentage of your balance a trade will be placed for
     * Example: 25.0 with $100 balance = trade placed for $25
     * If this turns out to be more than your MAX_USD_PER_TRADE, or
     * MIN_USD_PER_TRADE, those values will be used.
     * If -1, this is disabled and uses the MIN_USD_PER_TRADE */
    public double BUY_AMOUNT_PERCENT = 25.0;

    /** Don't enter a trade if there are more indicators signaling
     * sell than buy */
    public boolean DONT_BUY_IF_SELL_GREATER = true;

    /** Whether to sell all trades on one sell signal, or only close one per signal */
    public boolean SELL_ALL_ON_SIGNAL = false;

    /** The candlestick interval to trade on */
    public CandlestickInterval CANDLESTICK_INTERVAL = CandlestickInterval.FIVE_MINUTES;

    private final ArrayList<Indicator> indicators = new ArrayList<>();
    private String configName;


    public Strategy(String name, Indicator... indicators) {
        this(name, name.toLowerCase().replaceAll(" ", "_") + ".yml", indicators);
    }

    public Strategy(String name, String configFile, Indicator... indicators) {
        this.name = name;
        this.indicators.addAll(Arrays.asList(indicators));
        this.configName = configFile;

        StratX.log("Loading strategy settings from " + configFile);
        Configuration config = new Configuration("config/strategies/" + configFile);
        if (!config.exists()) {
            StratX.log("Could not find or load config, using built-in settings");
            configName = "(built-ins)";
            return;
        }

        loadSettings(config);
    }

    /** Called every time a price update is received */
    public void onPriceUpdate(double prevPrice, double newPrice) {}

    /** Called every time a candle is closed or every "tick" */
    public void onCandleClose(Candlestick candle) {
        for (Indicator indicator : indicators)
            indicator.update(candle);
    }

    /** Default implementation, uses the indicators to determine the signal
     * You may override this for custom strategies
     * When a candle closes, the mode calls this and if enough balance & not
     * too many open trades, a trade is placed based on this signal. */
    public Signal getSignal() {
        BuySellSignals buySellSignals = getBuySellSignals();
        int buySignals = buySellSignals.buySignals;
        int sellSignals = buySellSignals.sellSignals;

        if (buySignals == 0 && sellSignals == 0) return Signal.HOLD;
        if (buySignals > sellSignals || !DONT_BUY_IF_SELL_GREATER) return Signal.BUY;
        else if (sellSignals > buySignals) return Signal.SELL;
        else return Signal.HOLD;
    }

    /** Called to determine if a buy position can be opened. Should generally be kept
     * the same. This verifies that there is not more than the max open trades,
     * buy signals > than sell signal count, etc. */
    public boolean isValidBuy(double amtUSD) {
        BuySellSignals buySellSignals = getBuySellSignals();
        int buySignals = buySellSignals.buySignals;
        int sellSignals = buySellSignals.sellSignals;

        return (((MIN_BUY_SIGNALS == -1 && buySignals >= indicators.size()) || (buySignals >= MIN_BUY_SIGNALS && MIN_BUY_SIGNALS != -1))
                && buyRequirementsMet()
                && (buySignals >= sellSignals && DONT_BUY_IF_SELL_GREATER)
                && (StratX.getCurrentMode().getAccount().getOpenTrades() < MAX_OPEN_TRADES)
                && (StratX.getCurrentMode().getAccount().getBalance() > 0)
                && amtUSD >= MIN_USD_PER_TRADE
        );
    }

    private boolean buyRequirementsMet() {
        for (Indicator indicator : indicators)
            if (indicator.isRequiredForBuy() && indicator.getSignal() != Signal.BUY) return false;

        return true;
    }

    /** Called to determine if a sell position can be opened. Should generally be kept
     * the same. */
    public boolean isValidSell() {
        int sellSignals = getBuySellSignals().sellSignals;
        return (((MIN_SELL_SIGNALS == -1 && sellSignals >= indicators.size()) || (sellSignals >= MIN_SELL_SIGNALS && MIN_SELL_SIGNALS != -1))
                && sellRequirementsMet()
                && SELL_BASED_ON_INDICATORS
                && StratX.getCurrentMode().getAccount().getOpenTrades() > 0
        );
    }

    private boolean sellRequirementsMet() {
        for (Indicator indicator : indicators)
            if (indicator.isRequiredForSell() && indicator.getSignal() != Signal.SELL) return false;

        return true;
    }

    private BuySellSignals getBuySellSignals() {
        int buySignals = 0;
        int sellSignals = 0;

        for (Indicator indicator : indicators) {
            Signal signal = indicator.getSignal();
            if (signal == Signal.BUY) buySignals++;
            else if (signal == Signal.SELL) sellSignals++;
        }

        return new BuySellSignals(buySignals, sellSignals);
    }

    /** Called when a trade is opened to determine how much USD
     * it should be bought for. You may override this for custom implementations. */
    public double getBuyAmount() {
        double bal = StratX.getCurrentMode().getAccount().getBalance();
        double buy = MIN_USD_PER_TRADE;

        // Percentage buy
        if (BUY_AMOUNT_PERCENT > 0)
            buy = bal * (BUY_AMOUNT_PERCENT / 100.0D);

        return MathUtils.clampDouble(buy, Math.min(MIN_USD_PER_TRADE, bal), MAX_USD_PER_TRADE == -1 ? Double.MAX_VALUE : MAX_USD_PER_TRADE);
    }

    private void loadSettings(Configuration config) {
        MAX_OPEN_TRADES = config.getInt("max-open-trades", MAX_OPEN_TRADES);
        CLOSE_OPEN_TRADES_ON_EXIT = config.getBoolean("close-open-trades-on-exit", CLOSE_OPEN_TRADES_ON_EXIT);
        USE_TAKE_PROFIT = config.getBoolean("take-profit.enabled", USE_TAKE_PROFIT);
        TAKE_PROFIT = config.getDouble("take-profit.percent", TAKE_PROFIT);
        USE_STOP_LOSS = config.getBoolean("stop-loss.enabled", USE_STOP_LOSS);
        STOP_LOSS = config.getDouble("stop-loss.percent", STOP_LOSS);
        USE_TRAILING_STOP = config.getBoolean("trailing-stop.enabled", USE_TRAILING_STOP);
        ARM_TRAILING_STOP_AT = config.getDouble("trailing-stop.arm-at", ARM_TRAILING_STOP_AT);
        TRAILING_STOP = config.getDouble("trailing-stop.percent", TRAILING_STOP);
        MIN_BUY_SIGNALS = config.getInt("buy.min-signals", MIN_BUY_SIGNALS);
        BUY_AMOUNT_PERCENT = config.getDouble("buy.percent-of-bal", BUY_AMOUNT_PERCENT);
        DONT_BUY_IF_SELL_GREATER = config.getBoolean("buy.dont-buy-if-more-sell-signals", DONT_BUY_IF_SELL_GREATER);
        MIN_USD_PER_TRADE = config.getDouble("buy.min-usd", MIN_USD_PER_TRADE);
        MAX_USD_PER_TRADE = config.getDouble("buy.max-usd", MAX_USD_PER_TRADE);
        MIN_SELL_SIGNALS = config.getInt("sell.min-signals", MIN_SELL_SIGNALS);
        SELL_BASED_ON_INDICATORS = config.getBoolean("sell.based-on-indicators", SELL_BASED_ON_INDICATORS);
        SELL_ALL_ON_SIGNAL = config.getBoolean("sell.sell-all", SELL_ALL_ON_SIGNAL);

        for (String indStr : config.getStringList("buy.required"))
            for (Indicator ind : indicators)
                if (ind.getName().equals(indStr)) ind.setRequiredForBuy(true);

        for (String indStr : config.getStringList("sell.required"))
            for (Indicator ind : indicators)
                if (ind.getName().equals(indStr)) ind.setRequiredForSell(true);

        String interval = config.getString("candlestick-interval");
        if (interval == null) interval = "FIVE_MINUTES";

        try {
            CANDLESTICK_INTERVAL = CandlestickInterval.valueOf(interval.toUpperCase());
        } catch (Exception ignored) {}

        StratX.log("Successfully loaded strategy settings");
    }

    public void addIndicator(Indicator indicator) {
        indicators.add(indicator);
    }

    public void removeIndicator(Indicator indicator) {
        indicators.remove(indicator);
    }

    public ArrayList<Indicator> getIndicators() {
        return indicators;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Config File: ").append(configName).append("\n");
        sb.append("Strategy: ").append(name).append("\n");
        sb.append("MAX_OPEN_TRADES: ").append(MAX_OPEN_TRADES).append("\n");
        sb.append("USE_TAKE_PROFIT: ").append(USE_TAKE_PROFIT).append("\n");
        sb.append("TAKE_PROFIT: ").append(TAKE_PROFIT).append("\n");
        sb.append("USE_STOP_LOSS: ").append(USE_STOP_LOSS).append("\n");
        sb.append("STOP_LOSS: ").append(STOP_LOSS).append("\n");
        sb.append("USE_TRAILING_STOP: ").append(USE_TRAILING_STOP).append("\n");
        sb.append("ARM_TRAILING_STOP_AT: ").append(ARM_TRAILING_STOP_AT).append("\n");
        sb.append("TRAILING_STOP: ").append(TRAILING_STOP).append("\n");
        sb.append("SELL_BASED_ON_INDICATORS: ").append(SELL_BASED_ON_INDICATORS).append("\n");
        sb.append("CLOSE_OPEN_TRADES_ON_EXIT: ").append(CLOSE_OPEN_TRADES_ON_EXIT).append("\n");
        sb.append("MIN_BUY_SIGNALS: ").append(MIN_BUY_SIGNALS).append("\n");
        sb.append("MIN_SELL_SIGNALS: ").append(MIN_SELL_SIGNALS).append("\n");
        sb.append("MAX_USD_PER_TRADE: ").append(MAX_USD_PER_TRADE).append("\n");
        sb.append("MIN_USD_PER_TRADE: ").append(MIN_USD_PER_TRADE).append("\n");
        sb.append("BUY_AMOUNT_PERCENT: ").append(BUY_AMOUNT_PERCENT).append("\n");
        sb.append("DONT_BUY_IF_SELL_GREATER: ").append(DONT_BUY_IF_SELL_GREATER).append("\n");
        sb.append("SELL_ALL_ON_SIGNAL: ").append(SELL_ALL_ON_SIGNAL).append("\n");
        sb.append("CANDLESTICK_INTERVAL: ").append(CANDLESTICK_INTERVAL).append("\n");
        
        return sb.toString();
    }
}
