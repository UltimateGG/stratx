package stratx.utils;

import java.text.DecimalFormat;

public class MathUtil {
	public static final DecimalFormat TWO_DEC = new DecimalFormat("##.00");
	public static final DecimalFormat COMMAS = new DecimalFormat("#");
	public static final DecimalFormat COMMAS_2F = new DecimalFormat("#.00");

	static {
		COMMAS.setGroupingUsed(true);
		COMMAS.setGroupingSize(3);
		COMMAS_2F.setGroupingUsed(true);
		COMMAS_2F.setGroupingSize(3);
	}
}
