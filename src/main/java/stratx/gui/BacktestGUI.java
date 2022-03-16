package stratx.gui;

import stratx.utils.Candlestick;

import javax.swing.*;
import java.net.URL;
import java.util.List;

public class BacktestGUI {
    private ChartRenderer chartRenderer;
    private JFrame frame;
    private final int width;
    private final int height;

    public BacktestGUI(String title, Candlestick.Interval interval, int width, int height) {
        this.width = width;
        this.height = height;
        createGUI(title, interval);
    }

    public void createGUI(String chartTitle, Candlestick.Interval interval) {
        frame = new JFrame("StratX Backtest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set icon
        URL iconURL = getClass().getResource("icon.png");
        if (iconURL != null) {
            ImageIcon icon = new ImageIcon(iconURL);
            frame.setIconImage(icon.getImage());
        }

        String chartName = "Backtest";
        try { // Standard backtest file name conversion
            String newName = chartTitle.substring(chartTitle.lastIndexOf("/") + 1).split("\\.")[0];
            newName = newName.replaceAll("_", " ");
            newName = newName.substring(0, newName.lastIndexOf(" ")) + " ";
            newName += interval.getValue() + " " + interval.toLongName();

            chartName = newName;
        } catch (Exception ignored) {}

        chartRenderer = new ChartRenderer(chartName, width, height);
        chartRenderer.setBackground(ChartRenderer.darkThemeColor);
    }

    public void populate(List<Candlestick> data, boolean autoScale, int maxCandles) {
        chartRenderer.populate(data, autoScale, maxCandles);
    }

    public void show() {
        SwingUtilities.invokeLater(this::setVisible); // New thread
    }

    private void setVisible() {
        assert frame != null;
        frame.setBackground(ChartRenderer.darkThemeColor);
        frame.setResizable(false);
        frame.setContentPane(chartRenderer);
        frame.pack();
        frame.setVisible(true);
    }
}