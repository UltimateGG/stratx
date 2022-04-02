package stratx;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import com.github.lgooddatepicker.components.DatePicker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import stratx.utils.MathUtils;
import stratx.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@SuppressWarnings("FieldCanBeLocal")
public class Downloader {
    private final Logger LOGGER = LogManager.getLogger("Downloader");
    private final BinanceApiRestClient CLIENT = BinanceApiClientFactory.newInstance().newRestClient();
    private final int MAX_CANDLES_PER_REQUEST = 1000; // Binance limitation
    private final String DATA_FOLDER = "src/main/resources/downloader/";
    private final int BREAK_SECONDS = 5;

    private boolean downloading = false;
    private JButton downloadButton;
    private JTextArea console;


    public static void main(String... args) {
        new Downloader().createAndShowGUI();
    }

    public void createAndShowGUI() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(450, 400));

        JTextField symbolField = new JTextField("BTCUSDT");
        setupComponent(symbolField, "Symbol", panel);

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.getComponentToggleCalendarButton().setContentAreaFilled(false);
        startDatePicker.setDateToToday();
        startDatePicker.setDate(startDatePicker.getDate().minusDays(31));
        setupComponent(startDatePicker, "Start Date", panel);

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setDateToToday();
        endDatePicker.getComponentToggleCalendarButton().setContentAreaFilled(false);
        setupComponent(endDatePicker, "End Date", panel);

        JComboBox<CandlestickInterval> intervalBox = new JComboBox<>(CandlestickInterval.values());
        intervalBox.setSelectedItem(CandlestickInterval.FIVE_MINUTES);
        setupComponent(intervalBox, "Interval", panel);

        downloadButton = new JButton("Download");
        setupComponent(downloadButton, null, panel);

        console = new JTextArea();
        console.setEditable(false);
        console.setLineWrap(true);
        console.setWrapStyleWord(true);
        console.setAutoscrolls(true);
        console.setFont(new Font("Courier New", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(console);
        scrollPane.setPreferredSize(new Dimension(panel.getPreferredSize().width - 30, 150));
        scrollPane.setSize(scrollPane.getPreferredSize());
        scrollPane.setMaximumSize(scrollPane.getPreferredSize());
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportView(console);
        scrollPane.setAutoscrolls(true);

        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(scrollPane);

        downloadButton.addActionListener(e -> {
            console.setText("");
            downloadButton.setEnabled(false);

            String symbol = symbolField.getText();
            long startTime = localDateToEpoch(startDatePicker.getDate());
            long endTime = localDateToEpoch(endDatePicker.getDate());
            CandlestickInterval interval = (CandlestickInterval) intervalBox.getSelectedItem();

            new Thread(() -> {
                try {
                    download(symbol, startTime, endTime, interval);
                } catch (Exception ex) {
                    log("ERROR: Caught exception during download: %s", ex);
                    LOGGER.error("ERROR: Caught exception during download: ", ex);
                    downloading = false;
                    downloadButton.setEnabled(true);
                }
            }).start();
        });

        JFrame frame = new JFrame("StratX Downloader");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setContentPane(panel);
        frame.pack();
        frame.setVisible(true);

        log("Downloader initialized");
    }

    private void setupComponent(JComponent component, String label, JPanel panel) {
        component.setFont(new Font("Arial", Font.PLAIN, 12));
        component.setForeground(Color.BLACK);
        component.setAlignmentX(Component.CENTER_ALIGNMENT);
        component.setPreferredSize(new Dimension(panel.getPreferredSize().width - 30, 35));
        component.setMaximumSize(component.getPreferredSize());
        component.setSize(component.getPreferredSize());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        if (component instanceof JTextField) {
            JTextField textField = (JTextField) component;
            textField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));

            textField.addFocusListener(new FocusListener() {
                @Override
                public void focusGained(FocusEvent e) {
                    textField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0x109CEC)));
                }

                @Override
                public void focusLost(FocusEvent e) {
                    textField.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK));
                }
            });
        } else if (component instanceof JButton) {
            JButton button = (JButton) component;
            button.setContentAreaFilled(false);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        if (label != null) {
            JLabel labelComponent = new JLabel(label);
            labelComponent.setFont(new Font("Arial", Font.PLAIN, 12));
            labelComponent.setForeground(Color.BLACK);
            labelComponent.setAlignmentX(Component.CENTER_ALIGNMENT);
            labelComponent.setPreferredSize(new Dimension(panel.getPreferredSize().width - 30, 30));
            labelComponent.setMaximumSize(labelComponent.getPreferredSize());
            labelComponent.setSize(labelComponent.getPreferredSize());
            panel.add(labelComponent);
        }

        panel.add(component);
    }

    private String createFile(String symbol, CandlestickInterval interval, long startTime) throws IOException {
        File dataFolder = new File(DATA_FOLDER);

        if (!dataFolder.exists()) {
            if (!dataFolder.mkdirs()) {
                log("ERROR: Failed to create data folder: " + dataFolder.getAbsolutePath());
                throw new IOException("Failed to create data folder: " + dataFolder.getAbsolutePath());
            }
        }

        String fileName = String.format("%s%s_%s_%s", DATA_FOLDER, symbol, interval.getIntervalId(), formatDate(new Date(startTime)));
        if (new File(fileName + ".strx").exists()) fileName += "_" + Utils.getRandomString(5);
        fileName += ".strx";
        File dataFile = new File(fileName);

        if (dataFolder.exists() && dataFolder.isDirectory() && !dataFile.exists() && dataFile.createNewFile()) {
            log("Created data file: " + dataFile.getAbsolutePath());
            return dataFile.getAbsolutePath();
        }

        throw new IOException("Failed to create data file: " + dataFile.getAbsolutePath());
    }

    public void download(String symbol, final long startTime, final long endTime, CandlestickInterval interval) throws IOException {
        if (downloading) throw new IllegalStateException("Already downloading");
        String fileName = createFile(symbol, interval, startTime);
        downloading = true;

        int totalCandles = (int) ((endTime - startTime) / Utils.binanceIntervalToMs(interval));
        int numRequests = MathUtils.clampInt(totalCandles / MAX_CANDLES_PER_REQUEST, 1, Integer.MAX_VALUE);

        if (!isValidSymbol(symbol)) {
            log("ERROR: Invalid symbol: " + symbol);
            System.exit(1);
        }

        log("Downloading %s on %s interval (%s candlesticks/%s requests)..", symbol, interval.getIntervalId(), MathUtils.COMMAS.format(totalCandles), numRequests);

        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fileName)))) {
            long currentStartTime = startTime;

            // the magic string is 'b4 ff b4 ff' + the version number
            output.writeByte(0xb4);
            output.writeByte(0xff);
            output.writeByte(0xb4);
            output.writeByte(0xff);
            output.writeByte(0x01);

            output.writeLong(startTime);
            output.writeLong(endTime);

            List<Long> times = new ArrayList<>();
            for (int i = 0; i < numRequests; i++) {
                List<Candlestick> downloaded = CLIENT.getCandlestickBars(symbol, interval, MAX_CANDLES_PER_REQUEST, currentStartTime, endTime);

                // Date Open High Low Close Volume
                for (Candlestick candle : downloaded) {
                    if (times.contains(candle.getCloseTime())) {
                        log("WARN: Duplicate candle time: " + candle.getCloseTime());
                        continue;
                    }

                    times.add(candle.getCloseTime());
                    output.writeLong(candle.getCloseTime());
                    output.writeDouble(Double.parseDouble(candle.getOpen()));
                    output.writeDouble(Double.parseDouble(candle.getHigh()));
                    output.writeDouble(Double.parseDouble(candle.getLow()));
                    output.writeDouble(Double.parseDouble(candle.getClose()));
                    output.writeLong((long) Double.parseDouble(candle.getVolume()));
                }

                downloaded.clear();
                output.flush();
                currentStartTime += Utils.binanceIntervalToMs(interval) * MAX_CANDLES_PER_REQUEST;
                if (i % 5 == 0 && i > 0) {
                    log("Sent %d/%d requests, %s candles", i, numRequests, MathUtils.COMMAS.format((long) i * MAX_CANDLES_PER_REQUEST));
                    System.gc();
                }

                if (i % 15 == 0 && i > 0) {
                    log("Sleeping for %d seconds", BREAK_SECONDS);
                    try {
                        Thread.sleep(BREAK_SECONDS * 60 * 1000L);
                    } catch (Exception ignored) {}
                }
            }

            output.flush();
        }

        downloading = false;
        downloadButton.setEnabled(true);
        log("Download complete!");
    }

    private void log(String msg, Object... args) {
        LOGGER.info(String.format(msg, args));
        if (console != null) {
            console.append(String.format(msg, args) + "\n");
            console.setCaretPosition(console.getDocument().getLength());
        }
    }

    private boolean isValidSymbol(String symbol) {
        try {
            return CLIENT.get24HrPriceStatistics(symbol).getAskPrice() != null;
        } catch (Exception ignored) {}
        return false;
    }

    private String formatDate(Date d) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(d);
        return String.format("%s.%s.%s", cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.YEAR));
    }

    private long localDateToEpoch(LocalDate d) {
        return d.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public boolean isDownloading() {
        return downloading;
    }
}
