package stratx.gui;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;
import stratx.StratX;
import stratx.gui.candlestick.CandlestickChart;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.metal.MetalComboBoxButton;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.net.URL;
import java.time.LocalDate;

public class Gui {
    private final JFrame window;
    private CandlestickChart candlestickChart;
    private JPanel contentPanel;
    private final Border DEF_BORDER = BorderFactory.createCompoundBorder(
            new LineBorder(GuiTheme.SECONDARY_COLOR, 2),
            BorderFactory.createEmptyBorder(5, 10, 5, 10));

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
        candlestickChart = new CandlestickChart(
                chartTitle.substring(chartTitle.lastIndexOf('\\') + 1),
                window.getPreferredSize().width - 20,
                window.getPreferredSize().height - 40);

        window.setBackground(GuiTheme.PRIMARY_COLOR);
        window.setContentPane(candlestickChart);
    }

    public void show() {
        window.pack();
        window.setVisible(true);
    }

    public JPanel addPanel() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setPreferredSize(new Dimension(this.window.getPreferredSize().width, this.window.getPreferredSize().height));

        contentPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                contentPanel.requestFocusInWindow();
            }
        });

        this.styleComponent(contentPanel);
        window.setContentPane(contentPanel);
        return contentPanel;
    }

    public Gui addPaddingY(int padding) {
        contentPanel.add(Box.createRigidArea(new java.awt.Dimension(0, padding)));
        return this;
    }

    public JTextField addTextbox(String label, String defaultValue) {
        if (label != null) addLabel(label, 5);

        JTextField textbox = new JTextField(defaultValue);
        styleComponent(textbox);
        textbox.setFont(GuiTheme.FONT_SM);
        textbox.setCaretColor(GuiTheme.TEXT_COLOR);
        setTextboxHighlight(textbox, null);

        contentPanel.add(textbox);
        addPaddingY(10);
        return textbox;
    }

    public JComboBox<?> addDropdown(String label, JComboBox<?> dropdown, int selectedIndex) {
        if (label != null) addLabel(label, 5);

        styleComponent(dropdown);
        dropdown.setFont(GuiTheme.FONT_SM);
        dropdown.setSelectedIndex(selectedIndex);

        // get inner dropdown
        BasicComboPopup list = (BasicComboPopup) dropdown.getAccessibleContext().getAccessibleChild(0);
        list.getList().setSelectionBackground(GuiTheme.SECONDARY_COLOR);
        list.getList().setSelectionForeground(GuiTheme.TEXT_COLOR);
        list.getList().setBackground(GuiTheme.PRIMARY_COLOR);
        list.getList().setForeground(GuiTheme.TEXT_COLOR);
        list.getList().setFont(GuiTheme.FONT_SM);
        list.getList().setFocusable(false);
        list.setFocusable(false);

        for (int i = 0; i < dropdown.getComponentCount(); i++) {
            if (dropdown.getComponent(i) instanceof MetalComboBoxButton) {
               ((MetalComboBoxButton) dropdown.getComponent(i)).setBorderPainted(false);
            }
        }

        dropdown.setEditable(true);
        JTextField textField = (JTextField) dropdown.getEditor().getEditorComponent();
        textField.setBorder(BorderFactory.createEmptyBorder());
        textField.setBackground(GuiTheme.PRIMARY_COLOR);
        textField.setForeground(GuiTheme.TEXT_COLOR);
        textField.setFont(GuiTheme.FONT_SM);
        textField.setEditable(false);

        // on click open dropdown
        textField.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                dropdown.setPopupVisible(!dropdown.isPopupVisible());
            }
        });

        contentPanel.add(dropdown);
        addPaddingY(10);
        return dropdown;
    }

    public JButton addButton(String text, Color bgColor, Color textColor, Color borderColor, int padding) {
        JButton button = new JButton(text);
        styleComponent(button);
        Border border = borderColor == null ? BorderFactory.createEmptyBorder(padding, padding, padding, padding)
                : BorderFactory.createCompoundBorder(
                new LineBorder(borderColor, 2),
                BorderFactory.createEmptyBorder(padding, padding, padding, padding));

        button.setBorder(border);
        button.setFocusable(false);
        button.setForeground(textColor);
        button.setBackground(bgColor);

        // Listen for change to disabled
        button.addChangeListener(e -> {
            if (button.isEnabled()) {
                button.setBackground(bgColor);
                button.setForeground(textColor);
            } else {
                button.setBackground(GuiTheme.SECONDARY_COLOR);
                button.setForeground(GuiTheme.TEXT_COLOR);
            }
        });

        contentPanel.add(button);
        addPaddingY(20);
        return button;
    }

    public DatePicker addDatePicker(LocalDate initialDate, String label) {
        if (label != null) addLabel(label, 5);
        DatePicker datePicker = new DatePicker();

        if (initialDate == null) datePicker.setDateToToday();
        else datePicker.setDate(initialDate);

        datePicker.getComponentToggleCalendarButton().setContentAreaFilled(false);
        styleComponent(datePicker);

        datePicker.getSettings().setColor(DatePickerSettings.DateArea.TextFieldBackgroundValidDate, GuiTheme.PRIMARY_COLOR);
        datePicker.getSettings().setColor(DatePickerSettings.DateArea.TextFieldBackgroundInvalidDate, GuiTheme.SECONDARY_COLOR);
        datePicker.getSettings().setColor(DatePickerSettings.DateArea.DatePickerTextValidDate, GuiTheme.TEXT_COLOR);
        datePicker.getSettings().setFontValidDate(GuiTheme.FONT_SM);
        datePicker.getSettings().setFontInvalidDate(GuiTheme.FONT_SM);
        datePicker.getComponentDateTextField().setBorder(BorderFactory.createEmptyBorder());
        datePicker.getComponentDateTextField().setCaretColor(GuiTheme.TEXT_COLOR);
        datePicker.getComponentDateTextField().setFont(GuiTheme.FONT_SM);
        setTextboxHighlight(datePicker.getComponentDateTextField(), datePicker);

        datePicker.getComponentToggleCalendarButton().setBorder(BorderFactory.createEmptyBorder());
        datePicker.getComponentToggleCalendarButton().setForeground(GuiTheme.TEXT_COLOR);

        contentPanel.add(datePicker);
        addPaddingY(10);
        return datePicker;
    }

    public JTextArea addConsoleLog(int height) {
        JTextArea console = new JTextArea();
        console.setBackground(GuiTheme.PRIMARY_COLOR);
        console.setForeground(GuiTheme.TEXT_COLOR);
        console.setEditable(false);
        console.setLineWrap(true);
        console.setWrapStyleWord(true);
        console.setBorder(BorderFactory.createEmptyBorder());
        console.setFont(new Font("Courier New", Font.PLAIN, 12));

        DefaultCaret caret = (DefaultCaret) console.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(console);
        styleComponent(scrollPane);
        scrollPane.setPreferredSize(new Dimension(contentPanel.getPreferredSize().width - 30, height));
        scrollPane.setSize(scrollPane.getPreferredSize());
        scrollPane.setMaximumSize(scrollPane.getPreferredSize());
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setViewportView(console);
        scrollPane.setAutoscrolls(true);

        window.add(scrollPane);
        addPaddingY(10);
        return console;
    }

    public void addLabel(String label, int padding) {
        JLabel labelComponent = new JLabel(label);
        styleComponent(labelComponent);
        labelComponent.setBorder(BorderFactory.createEmptyBorder());
        addPaddingY(padding);
        contentPanel.add(labelComponent);
    }

    private void styleComponent(JComponent c) {
        c.setBackground(GuiTheme.PRIMARY_COLOR);
        c.setForeground(GuiTheme.TEXT_COLOR);
        c.setFont(GuiTheme.FONT_MD);
        c.setPreferredSize(new Dimension(contentPanel.getPreferredSize().width - 30, 35));
        c.setAlignmentX(Component.CENTER_ALIGNMENT);
        c.setMaximumSize(c.getPreferredSize());
        c.setSize(c.getPreferredSize());
        c.setBorder(DEF_BORDER);
    }

    private void setTextboxHighlight(JTextField textField, JComponent parent) {
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent evt) {
                ((parent != null) ? parent : textField).setBorder(BorderFactory.createCompoundBorder(
                        new LineBorder(GuiTheme.TEXT_COLOR, 2),
                        BorderFactory.createEmptyBorder(5, 10, 5, 10)));
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent evt) {
                ((parent != null) ? parent : textField).setBorder(DEF_BORDER);
            }
        });
    }

    public void setCloseOperation(int operation) {
        window.setDefaultCloseOperation(operation);
    }

    public JFrame getWindow() {
        return window;
    }

    public CandlestickChart getCandlestickChart() {
        return candlestickChart;
    }

    public void close() {
        window.dispose();
    }
}
