package stratx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.strategies.GridTrading;
import stratx.strategies.Strategy;

import java.io.File;
import java.util.Scanner;

public class StratX {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("StratX");
    public static final boolean DEVELOPMENT_MODE = "dev".equals(System.getProperty("env"));
    public static final String DATA_FOLDER = System.getProperty("user.dir")
            + (DEVELOPMENT_MODE ? "\\src\\main\\resources\\" : "\\stratx\\");

    static {
        if (!DEVELOPMENT_MODE) System.setProperty("env", "prod");
        org.fusesource.jansi.AnsiConsole.systemInstall();
    }

    // @TODO Temp entry point
    public static void main(String... args) {
        if (args.length == 0) {
            LOGGER.error("Usage: java -jar stratx.jar <mode>");
            return;
        }

        final String mode = args[0].toLowerCase().trim();

        if ("download".equals(mode)) downloaderMode();
        else if ("backtest".equals(mode)) backtestMode();
    }

    private static void downloaderMode() {
        LOGGER.info("Running downloader...");
        new Downloader().createAndShowGUI();
    }

    private static void backtestMode() {
        File[] files = new File(DATA_FOLDER + "\\downloader\\").listFiles();
        if (files == null) {
            LOGGER.error("No files found in {}, download them using the downloader mode!", DATA_FOLDER + "\\downloader\\");
            return;
        }

        LOGGER.info("Found {} files. Please select a file to test on.", files.length);
        for (int i = 1; i < files.length; i++) LOGGER.info("[{}] {}", i, files[i].getName());

        LOGGER.info("Number: ");
        Scanner scanner = new Scanner(System.in);
        int fileIndex = scanner.nextInt();

        if (fileIndex < 1 || fileIndex > files.length || !files[fileIndex].getName().endsWith(".strx")) {
            LOGGER.error("Invalid file index {}", fileIndex);
            return;
        }

        File file = files[fileIndex];
        LOGGER.info("Selected {}", file.getName());

        BackTest backtest = new BackTest(file.getAbsolutePath(), true);

        Strategy gridStrat = new GridTrading(backtest, 0.01);
        backtest.begin(gridStrat);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static void log(String msg, Object... args) {
        LOGGER.info(msg, args);
    }

    public static void warn(String msg, Object... args) {
        LOGGER.warn(msg, args);
    }

    public static void error(String msg, Throwable e) {
        LOGGER.error(msg, e);
    }

    public static void trace(String msg, Object... args) {
        LOGGER.trace(msg, args);
    }
}
