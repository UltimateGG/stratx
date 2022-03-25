package stratx.utils;

import com.binance.api.client.domain.market.CandlestickInterval;

public class Utils {
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static long binanceIntervalToMs(CandlestickInterval interval) {
        switch (interval) {
            case ONE_MINUTE:
                return 1000 * 60L;
            case THREE_MINUTES:
                return 1000 * 60 * 3L;
            case FIVE_MINUTES:
                return 1000 * 60 * 5L;
            case FIFTEEN_MINUTES:
                return 1000 * 60 * 15L;
            case HALF_HOURLY:
                return 1000 * 60 * 30L;
            case HOURLY:
                return 1000 * 60 * 60L;
            case TWO_HOURLY:
                return 1000 * 60 * 60 * 2L;
            case FOUR_HORLY:
                return 1000 * 60 * 60 * 4L;
            case SIX_HOURLY:
                return 1000 * 60 * 60 * 6L;
            case EIGHT_HOURLY:
                return 1000 * 60 * 60 * 8L;
            case TWELVE_HOURLY:
                return 1000 * 60 * 60 * 12L;
            case DAILY:
                return 1000 * 60 * 60 * 24L;
            case THREE_DAILY:
                return 1000 * 60 * 60 * 24 * 3L;
            case WEEKLY:
                return 1000 * 60 * 60 * 24 * 7L;
            case MONTHLY:
                return 1000 * 60 * 60 * 24 * 30L;
            default:
                return 0L;
        }
    }

    public static String getRandomString(int length) {
        StringBuilder sb = new StringBuilder();
        String characters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < length; i++) {
            int index = (int) (Math.random() * characters.length());
            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }
}
