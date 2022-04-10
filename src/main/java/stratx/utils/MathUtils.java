package stratx.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

public class MathUtils {
	public static final DecimalFormat TWO_DEC = new DecimalFormat("0.00");
	public static final DecimalFormat COMMAS = new DecimalFormat("#");
	public static final DecimalFormat COMMAS_2F = new DecimalFormat("0.00");

	static {
		COMMAS.setGroupingUsed(true);
		COMMAS.setGroupingSize(3);
		COMMAS_2F.setGroupingUsed(true);
		COMMAS_2F.setGroupingSize(3);
	}

	public static String formatPercent(double value) {
		return (value > 0 ? "+" : "") + COMMAS_2F.format(value) + "%";
	}

	public static String getPercent(double value, double total) {
		if (total == 0) return "0%";
		return formatPercent((value / total) * 100.0D);
	}

	public static double roundTwoDec(double value) {
		return Double.parseDouble(TWO_DEC.format(value));
	}

	public static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(value, max));
	}

	public static double clampDouble(double value, double min, double max) {
		return Math.max(min, Math.min(value, max));
	}

	public static float clampFloat(float value, float min, float max) {
		return Math.max(min, Math.min(value, max));
	}

	public static double round(double value, int places) {
		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}
}
