package stratx.gui;

import stratx.utils.Signal;

public class SignalIndicator {
    private final int candleID;
    private final Signal type;

    public SignalIndicator(int candleID, Signal type) {
        this.candleID = candleID;
        this.type = type;
    }

    public int getCandleID() {
        return candleID;
    }

    public Signal getType() {
        return type;
    }
}
