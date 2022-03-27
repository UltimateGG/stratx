package stratx.gui;

import javax.swing.*;
import java.io.File;
import java.net.URL;

public class BacktestGUI {
    private ChartRenderer chartRenderer;
    private JFrame frame;
    private final int width;
    private final int height;

    public BacktestGUI(String title, int width, int height) {
        this.width = width;
        this.height = height;
        createGUI(title);
    }

    public void createGUI(String chartTitle) {
        frame = new JFrame("StratX Backtest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set icon
        try {
            URL iconURL = new File("src/main/resources/icon.png").toURI().toURL();
            ImageIcon icon = new ImageIcon(iconURL);
            frame.setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        chartRenderer = new ChartRenderer(chartTitle.substring(chartTitle.lastIndexOf("/") + 1), width, height);
        chartRenderer.setBackground(ChartRenderer.darkThemeColor);
    }

    public void show() {
        SwingUtilities.invokeLater(this::setVisible); // New thread
    }

    public ChartRenderer getChartRenderer() {
        return chartRenderer;
    }

    private void setVisible() {
        assert frame != null;
        frame.setBackground(ChartRenderer.darkThemeColor);
        frame.setResizable(false);
        frame.setContentPane(chartRenderer);
        frame.pack();
        frame.setVisible(true);
    }

    public void dispose() {
        frame.dispose();
    }
}
