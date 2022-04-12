package stratx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.gui.Gui;
import stratx.gui.GuiTheme;
import stratx.indicators.Test;
import stratx.indicators.WMA;
import stratx.modes.*;
import stratx.strategies.GridTrading;
import stratx.strategies.Strategy;
import stratx.utils.BinanceClient;
import stratx.utils.Configuration;

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
            + (DEVELOPMENT_MODE ? "\\src\\main\\resources\\" : "\\");
    public static Mode.Type MODE = Mode.Type.SIMULATION;
    public static BinanceClient API = null;
    private static final Configuration CONFIG = new Configuration("config\\config.yml");
    private static Mode currentMode = null;

    public static void main(String... args) {
        if (System.getProperty("stratx.mode") != null) {
            launch(Mode.Type.valueOf(System.getProperty("stratx.mode").toUpperCase()));
        } else if (!GraphicsEnvironment.isHeadless()) {
            showSelectGui();
        } else {
            useCommandline();
        }
    }

    private StratX() {}

    private static void launch(Mode.Type mode) {
        MODE = mode;
        LOGGER.info("Starting StratX in {} mode...", MODE);

        if (MODE == Mode.Type.DOWNLOAD) {
            new Downloader().run();
            return;
        }

        // Log in to Binance
        if (MODE.requiresMarketDataStream()) {
            String loginFile = (DEVELOPMENT_MODE ? "dev-" : "") + "login.yml";
            Configuration loginConfig = new Configuration("config\\" + loginFile);
            API = new BinanceClient(loginConfig);
        }

        String coin = null;
        if (MODE.requiresMarketDataStream()) {
            coin = CONFIG.getString(MODE.getConfigKey() + ".coin");
            if (coin == null || coin.isEmpty()) {
                LOGGER.error("Coin is not set in config.yml - This is the coin the bot will trade.");
                System.exit(1);
            }

            LOGGER.info("Trading on {}", coin);
        }

        Strategy strat = new GridTrading(40);
        Strategy strat2 = new Strategy("Test", new Test());
        Strategy emaStrat = new Strategy("WMA", "test.yml", new WMA(60));

        if (MODE == Mode.Type.BACKTEST) currentMode = new BackTest(emaStrat);
        else if (MODE == Mode.Type.SIMULATION) currentMode = new Simulation(emaStrat, coin);
        else if (MODE == Mode.Type.LIVE) currentMode = new LiveTrading(strat, coin);
        else {
            LOGGER.error("Invalid mode {}", MODE);
            System.exit(1);
        }

        try { // Backtest has to run through file picker GUI first, then starts itself
            if (MODE != Mode.Type.BACKTEST) currentMode.begin();
        } catch (Exception e) {
            LOGGER.error("Error while running mode {}", MODE, e);
        }
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

    public static Configuration getConfig() {
        return CONFIG;
    }

    public static Mode getCurrentMode() {
        return currentMode;
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
