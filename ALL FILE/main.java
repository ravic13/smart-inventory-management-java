import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Main Application Entry Point
 * Starts the Inventory Management System with standard Look and Feel
 */
public class main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> {
            new Login();
        });
    }
}
