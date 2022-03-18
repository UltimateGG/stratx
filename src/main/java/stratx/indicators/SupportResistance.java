package stratx.indicators;

import stratx.utils.Candlestick;
import stratx.utils.Signal;

import java.util.ArrayList;

public class SupportResistance implements Indicator {
    private final double sensitivity;

    private final ArrayList<SRLevel> levels = new ArrayList<>();
    private final ArrayList<Double> closingPrices = new ArrayList<>();

    public SupportResistance(double sensitivity) {
        this.sensitivity = sensitivity;
    }

    @Override
    public String getName() {
        return "S&R";
    }

    @Override
    public void update(Candlestick candle) {
        closingPrices.add(candle.getClose());
        search();
    }

    /**
     * Search through every candle to find the support and resistance levels
     * with the most hits on a y level
     */
    private void search() {
        int maxHits = 0;
        int maxHitsIndex = 0;
        for (int i = 0; i < closingPrices.size(); i++) {
            int hits = 0;
            for (int j = 0; j < closingPrices.size(); j++) {
                if (i != j) {
                    if (Math.abs(closingPrices.get(i) - closingPrices.get(j)) < sensitivity)
                        hits++;
                }
            }
            if (hits > maxHits) {
                maxHits = hits;
                maxHitsIndex = i;
            }
        }

        if (maxHits < 2) return;

        // Dont add if close enough to sensitivity of another level
        for (SRLevel level : levels) {
            if (Math.abs(closingPrices.get(maxHitsIndex) - level.getLevel()) < sensitivity)
                return;
        }

        levels.add(new SRLevel(maxHits, closingPrices.get(maxHitsIndex), SRType.SUPPORT));
        int bestLevel = 0;
        for (SRLevel level : levels) {
            if (level.getStrength() > levels.get(bestLevel).getStrength()) {
                bestLevel = levels.indexOf(level);
            }
        }

        System.out.println("Best level: " + levels.get(bestLevel));
        System.out.println(levels.size());
    }

    @Override
    public Signal getSignal() {
        return Signal.HOLD;
    }

    public static final class SRLevel {
        private final int strength;
        private final double level;
        private final SRType type;

        public SRLevel(int strength, double level, SRType type) {
            this.strength = strength;
            this.level = level;
            this.type = type;
        }

        public int getStrength() {
            return strength;
        }

        public double getLevel() {
            return level;
        }

        public SRType getType() {
            return type;
        }
        public String toString() {
            return type.toString() + ": " + strength + " " + level;
        }
    }

    public static enum SRType { SUPPORT, RESISTANCE; }
}
