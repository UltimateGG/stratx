package stratx;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.gui.Gui;
import stratx.gui.GuiTheme;
import stratx.indicators.HeikinAshiTrends;
import stratx.indicators.RSI;
import stratx.indicators.WMA;
import stratx.modes.*;
import stratx.strategies.Strategy;
import stratx.strategies.TradingViewHook;
import stratx.utils.BinanceClient;
import stratx.utils.Configuration;
import stratx.utils.CurrencyPair;
import stratx.utils.Utils;

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
    static { Utils.trySetup(); }
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

        CurrencyPair coin = null;
        if (MODE.requiresMarketDataStream()) {
            String crypto = CONFIG.getString("crypto");
            String fiat = CONFIG.getString("fiat");
            if (crypto == null || fiat == null || crypto.isEmpty() || fiat.isEmpty()) {
                LOGGER.error("Crypto or asset is not set in config.yml - This is the currency pair the bot will trade.");
                System.exit(1);
            }

            coin = new CurrencyPair(crypto, fiat);
            LOGGER.info("Trading on {}", coin.toString());
        }

        Strategy heikinTrading = new Strategy("Heikin Ashi Trend Follower", "ha_trend.yml",
                new HeikinAshiTrends(),
                new RSI(14, 70, 30), new WMA(30)
        );
        Strategy tvHook = new TradingViewHook("tradingview.yml");

        if (MODE == Mode.Type.BACKTEST) currentMode = new BackTest(tvHook);
        else if (MODE == Mode.Type.SIMULATION) currentMode = new Simulation(tvHook, coin);
        else if (MODE == Mode.Type.LIVE) currentMode = new LiveTrading(tvHook, coin);
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

    public static void both(String msg, Object... args) {
        LOGGER.info(msg, args);
        LOGGER.trace(msg, args);
    }
}
