package stratx.utils;

import java.util.ArrayList;
import java.util.List;

public class PriceHistory {
    private int maxLength;
    private final List<Candlestick> history = new ArrayList<>();

    public PriceHistory(int maxLength) {
        this.maxLength = maxLength;
    }

    public void add(Candlestick candlestick) {
        history.add(candlestick);
        if (history.size() > maxLength) history.remove(0);
    }

    public int length() {
        return history.size();
    }

    public List<Candlestick> get() {
        return history;
    }

    public Candlestick get(int index) {
        return history.get(index);
    }

    public void clear() {
        history.clear();
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }
}
