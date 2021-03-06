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
        if (history.contains(candlestick)) throw new IllegalArgumentException("Candlestick already exists in history");
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

    public Candlestick getLatest() {
        if (history.size() == 0) return null;
        return history.get(history.size() - 1);
    }

    public Candlestick getByTime(long closingTime) {
        for (Candlestick candlestick : history)
            if (candlestick.getCloseTime() == closingTime) return candlestick;

        return null;
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
