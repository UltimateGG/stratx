package stratx.gui;

public class GuiFactory {
    public static Gui createGui(String title, int width, int height) {
        return new Gui(title, width, height, true);
    }

    public static Gui createStaticGui(String title, int width, int height) {
        return new Gui(title, width, height, false);
    }
}
