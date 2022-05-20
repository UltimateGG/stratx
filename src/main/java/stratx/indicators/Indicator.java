package stratx.indicators;


import stratx.utils.Candlestick;
import stratx.utils.PriceHistory;
import stratx.utils.Signal;

public abstract class Indicator {
    protected String name;
    private boolean isRequiredForBuy = false;
    private boolean isRequiredForSell = false;

    protected Indicator(String name) {
        this.name = name;
    }

    protected PriceHistory priceHistory = null;

    public abstract void update(Candlestick candle);

    public abstract Signal getSignal();

    public PriceHistory getPriceHistory() {
        return priceHistory;
    }

    public String getName() {
        return name;
    }

    public boolean isRequiredForBuy() {
        return isRequiredForBuy;
    }

    public void setRequiredForBuy(boolean requiredForBuy) {
        this.isRequiredForBuy = requiredForBuy;
    }

    public boolean isRequiredForSell() {
        return isRequiredForSell;
    }

    public void setRequiredForSell(boolean requiredForSell) {
        this.isRequiredForSell = requiredForSell;
    }
}
