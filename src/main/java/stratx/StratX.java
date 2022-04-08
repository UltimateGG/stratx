package stratx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.gui.Gui;
import stratx.gui.GuiTheme;
import stratx.modes.*;
import stratx.strategies.GridTrading;
import stratx.strategies.Strategy;
import stratx.utils.Configuration;
import stratx.utils.binance.BinanceClient;

import javax.swing.*;
import java.awt.*;
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
    public static Mode.Type MODE = Mode.Type.SIMULATION;
    public static BinanceClient API = null;
    private static final Configuration CONFIG = new Configuration("config\\config.yml");

    public static void main(String... args) {
        new StratX();
    }

    private StratX() {
        if (System.getProperty("stratx.mode") != null) {
            launch(Mode.Type.valueOf(System.getProperty("stratx.mode").toUpperCase()));
        } else if (!GraphicsEnvironment.isHeadless()) {
            showSelectGui();
        } else {
            useCommandline();
        }
    }

    private static void launch(Mode.Type mode) {
        MODE = mode;
        LOGGER.info("Starting StratX in {} mode...", MODE);

        // Log in to Binance
        if (MODE.requiresMarketDataStream())
            API = new BinanceClient(new Configuration("config\\"
                    + (DEVELOPMENT_MODE ? "dev-" : "")
                    + "login.yml"));

        final String coin = CONFIG.getString("coin");
        if (coin == null) {
            LOGGER.error("Coin is not set in config.yml");
            System.exit(1);
        }

        if (MODE.requiresMarketDataStream()) LOGGER.info("Trading {}", coin);

        Mode runningMode = null;
        if (MODE == Mode.Type.BACKTEST) runningMode = new BackTest();
        else if (MODE == Mode.Type.DOWNLOAD) runningMode = new Downloader();
        else if (MODE == Mode.Type.SIMULATION) runningMode = new Simulation(coin);
        else if (MODE == Mode.Type.LIVE) runningMode = new LiveTrading(coin);
        else {
            LOGGER.error("Invalid mode {}", MODE);
            System.exit(1);
        }

        Strategy strat = new GridTrading(runningMode, 0.01);
        runningMode.setStrategy(strat);
    }

    private static void useCommandline() {
        Scanner scanner = new Scanner(System.in);
        LOGGER.info("Please select a mode: "  + Arrays.toString(Mode.Type.values()));

        String mode = scanner.next();
        launch(Mode.Type.valueOf(mode.toUpperCase()));
    }

    private static void showSelectGui() {
        Gui selectGui = new Gui("StratX", 300, 200, false);

        selectGui.addPanel();
        JComboBox<?> modeBox = selectGui.addDropdown("Select Mode", new JComboBox<>(Mode.Type.values()), 0);
        selectGui.addPaddingY(10);
        selectGui.addButton("Start", GuiTheme.INFO_COLOR, GuiTheme.TEXT_COLOR, null, 10).addActionListener(e -> {
            Mode.Type type = (Mode.Type) modeBox.getSelectedItem();
            selectGui.close();
            launch(type);
        });

        selectGui.show();
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
