package stratx.gui;

import stratx.BackTest;
import stratx.StratX;

import javax.swing.*;
import java.net.URL;

public class BacktestGUI {
    private ChartRenderer chartRenderer;
    private JFrame frame;
    private final int width;
    private final int height;

    public BacktestGUI(String title, BackTest simulation, int width, int height) {
        this.width = width;
        this.height = height;
        createGUI(title, simulation);
    }

    public void createGUI(String chartTitle, BackTest simulation) {
        frame = new JFrame("StratX Backtest");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try { // Set icon
            URL iconURL = StratX.class.getResource("/icon.png");
            assert iconURL != null;
            ImageIcon icon = new ImageIcon(iconURL);
            frame.setIconImage(icon.getImage());
        } catch (Exception ignored) {}

        chartRenderer = new ChartRenderer(chartTitle.substring(chartTitle.lastIndexOf('\\') + 1), simulation, width, height);
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
