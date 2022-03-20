package stratx.indicators;

import stratx.BackTest;

public class Indicator {
    protected final BackTest simulation;

    public Indicator(BackTest simulation) {
        this.simulation = simulation;
    }
}
