package stratx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.strategies.GridTrading;
import stratx.strategies.Strategy;
import stratx.utils.Configuration;
import stratx.utils.Mode;
import stratx.utils.binance.BinanceClient;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

public class StratX {
    public static final boolean DEVELOPMENT_MODE = "dev".equals(System.getProperty("env"));
    static {
        if (!DEVELOPMENT_MODE) System.setProperty("env", "prod");
        org.fusesource.jansi.AnsiConsole.systemInstall();
    }

    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger("StratX");
    public static final String DATA_FOLDER = System.getProperty("user.dir")
            + (DEVELOPMENT_MODE ? "\\src\\main\\resources\\" : "\\stratx\\");
    public static Mode MODE = Mode.SIMULATION;
    public static BinanceClient API = null;

    public static void main(String... args) {
        new StratX();
    }

    private StratX() {
        Scanner scanner = new Scanner(System.in);
        LOGGER.info("Please select a mode: "  + Arrays.toString(Mode.values()));

        MODE = Mode.valueOf(scanner.next().toUpperCase().trim());
        LOGGER.info("Starting StratX in {} mode...", MODE);

        // Log in to Binance
        if (MODE != Mode.BACKTEST && MODE != Mode.DOWNLOAD)
            API = new BinanceClient(new Configuration("config\\"
                + (DEVELOPMENT_MODE ? "dev-" : "")
                + "login.yml"));

        if (true) {
            Simulation.main(new String[]{});
            return;
        }
        if (MODE == Mode.DOWNLOAD) {
            new Downloader().createAndShowGUI();
        } else if (MODE == Mode.BACKTEST) backtestMode();
        else if (MODE == Mode.SIMULATION) {

        } else if (MODE == Mode.LIVE) {

        } else {
            LOGGER.error("Invalid mode {}", MODE);
        }
    }

    private static void backtestMode() {
        File backtestData = getFileFromUser();
        BackTest backtest = new BackTest(backtestData.getAbsolutePath(), true);

        Strategy gridStrat = new GridTrading(backtest, 0.01);
        backtest.begin(gridStrat);
    }

    private static File getFileFromUser() {
        File[] files = new File(DATA_FOLDER + "\\downloader\\").listFiles();

        if (files == null || files.length == 0) {
            LOGGER.error("No files found in {}, download them using the downloader mode!", DATA_FOLDER + "\\downloader\\");
            System.exit(1);
        }

        LOGGER.info("Found {} files. Please select a file to test on:", files.length);
        for (int i = 1; i < files.length; i++) LOGGER.info("[{}] {}", i, files[i].getName());

        LOGGER.info("Number: "); // @TODO config for max open trade time?, market data stream
        Scanner scanner = new Scanner(System.in);
        int fileIndex = scanner.nextInt();

        if (fileIndex < 1 || fileIndex > files.length || !files[fileIndex].getName().endsWith(".strx")) {
            LOGGER.error("Invalid file index {}", fileIndex);
            System.exit(1);
        }

        File file = files[fileIndex];
        LOGGER.info("Selected {}", file.getName());

        return file;
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
