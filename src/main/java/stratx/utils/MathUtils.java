package stratx.utils;

import java.text.DecimalFormat;

public class MathUtils {
	public static final DecimalFormat TWO_DEC = new DecimalFormat("##.00");
	public static final DecimalFormat COMMAS = new DecimalFormat("#");
	public static final DecimalFormat COMMAS_2F = new DecimalFormat("#.00");

	static {
		COMMAS.setGroupingUsed(true);
		COMMAS.setGroupingSize(3);
		COMMAS_2F.setGroupingUsed(true);
		COMMAS_2F.setGroupingSize(3);
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
}
