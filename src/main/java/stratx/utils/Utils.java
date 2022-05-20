package stratx.utils;

import com.binance.api.client.domain.general.FilterType;
import com.binance.api.client.domain.general.SymbolFilter;
import com.binance.api.client.domain.market.CandlestickInterval;
import stratx.StratX;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;

import static stratx.StratX.DEVELOPMENT_MODE;

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
            case FOUR_HOURLY:
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

    public static String msToNice(long ms) {
        return msToNice(ms, true, true, true);
    }

    public static String msToNice(long ms, boolean showSeconds) {
        return msToNice(ms, true, true, showSeconds);
    }

    public static String msToNice(long ms, boolean showHours, boolean showMinutes, boolean showSeconds) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long years = days / 365;

        String result = "";

        if (years > 0) result += years + "y ";
        if (days > 0)  result += (days % 365) + "d ";
        if (hours > 0 && showHours) result += (hours % 24) + "h ";
        if (minutes > 0 && showMinutes) result += (minutes % 60) + "m ";
        if (seconds > 0 && showSeconds) result += (seconds % 60) + "s ";

        return result;
    }

    public static boolean isFirstRun() {
        return !new File(StratX.DATA_FOLDER, "config\\config.yml").exists() && !DEVELOPMENT_MODE;
    }

    public static void trySetup() {
        if (!isFirstRun()) return;
        StratX.log("Extracting config files...");
        if (!new File(StratX.DATA_FOLDER + "config").mkdirs() || !new File(StratX.DATA_FOLDER + "config\\strategies\\").mkdirs()) {
            StratX.log("Failed to create config folders");
            System.exit(1);
        }

        final String[] configFiles = {
                "/config/config.yml",
                "/config/login.yml",
                "/config/strategies/grid.yml"
        };

        try {
            for (String configFile : configFiles)
                copyFile(Objects.requireNonNull(StratX.class.getResource(configFile)).openStream(), StratX.DATA_FOLDER + configFile);
        } catch (Exception e) {
            StratX.log("Failed to extract config files");
            e.printStackTrace();
            System.exit(1);
        }

        StratX.log("Setup files have successfully been extracted");
    }

    public static void copyFile(InputStream in, String target) {
        try {
            java.nio.file.Files.copy(in, java.nio.file.Paths.get(target), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String convertTradeAmount(double amount, String currency) {
        BigDecimal originalDecimal = BigDecimal.valueOf(amount);
        int precision = StratX.API.get().getExchangeInfo().getSymbolInfo(currency).getBaseAssetPrecision(); // Round amount to base precision and LOT_SIZE
        String lotSize;
        Optional<String> minQtyOptional = StratX.API.get().getExchangeInfo().getSymbolInfo(currency)
                .getFilters().stream().filter(f -> FilterType.LOT_SIZE == f.getFilterType()).findFirst().map(SymbolFilter::getMinQty);
        Optional<String> minNotational = StratX.API.get().getExchangeInfo().getSymbolInfo(currency)
                .getFilters().stream().filter(f -> FilterType.MIN_NOTIONAL == f.getFilterType()).findFirst().map(SymbolFilter::getMinNotional);

        if (minQtyOptional.isPresent()) {
            lotSize = minQtyOptional.get();
        } else {
            StratX.getLogger().error("Could not find lot size for {}, could not place trade.", currency);
            return null;
        }

        double minQtyDouble = Double.parseDouble(lotSize);
        if (amount < minQtyDouble) { // Check LOT_SIZE to make sure amount is not too small
            StratX.getLogger().error("Amount smaller than min LOT_SIZE, could not open trade! (min LOT_SIZE={}, amount={})", lotSize, amount);
            return null;
        }

        // Convert amount to an integer multiple of LOT_SIZE and convert to asset precision
        String convertedAmount = new BigDecimal(lotSize).multiply(new BigDecimal((int) (amount / minQtyDouble))).setScale(precision, RoundingMode.HALF_DOWN).toString();

        if (minNotational.isPresent()) {
            double notational = Double.parseDouble(convertedAmount) * StratX.getCurrentMode().getCurrentPrice();
            if (notational < Double.parseDouble(minNotational.get())) {
                StratX.getLogger().error("Notational value {} is smaller than minimum {}", MathUtils.roundTwoDec(notational), minNotational.get());
                return null;
            }
        }

        StratX.trace("Converted amount {} to {}",
                MathUtils.round(Double.parseDouble(originalDecimal.toString()), precision),
                MathUtils.round(Double.parseDouble(convertedAmount), precision));
        return convertedAmount;
    }
}
