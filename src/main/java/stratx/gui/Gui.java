package stratx.gui;

import stratx.StratX;

import javax.swing.*;
import java.net.URL;

public class Gui {
    private final JFrame window;
    private CandlestickChartRenderer candlestickChart;

    public Gui(String title, int width, int height, boolean resizable) {
        window = new JFrame(title);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        window.setSize(width, height);
        window.setPreferredSize(new java.awt.Dimension(width, height));
        window.setResizable(resizable);
    }

    public boolean setIcon(String path) {
        try { // Set icon
            URL iconURL = StratX.class.getResource(path);
            assert iconURL != null;
            ImageIcon icon = new ImageIcon(iconURL);
            window.setIconImage(icon.getImage());
            return true;
        } catch (Exception ignored) {}

        return false;
    }

    public void addCandlestickChart(String chartTitle) {
        candlestickChart = new CandlestickChartRenderer(
                chartTitle.substring(chartTitle.lastIndexOf('\\') + 1),
                window.getPreferredSize().width,
                window.getPreferredSize().height );

        window.setBackground(GuiTheme.PRIMARY_COLOR);
        window.setContentPane(candlestickChart);
    }

    public void show() {
        window.pack();
        window.setVisible(true);
    }

    public Gui addPaddingY(int padding) {
        window.add(Box.createRigidArea(new java.awt.Dimension(0, padding)));
        return this;
    }

    public void setCloseOperation(int operation) {
        window.setDefaultCloseOperation(operation);
    }

    public JFrame getWindow() {
        return window;
    }

    public CandlestickChartRenderer getCandlestickChart() {
        return candlestickChart;
    }

    public void close() {
        window.dispose();
    }
}
