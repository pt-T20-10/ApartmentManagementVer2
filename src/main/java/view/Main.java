package view;

import javax.swing.*;

/**
 * Main Entry Point Starts the application with login screen
 */
public class Main {

    public static void main(String[] args) {
        // Set look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Start with login screen
        SwingUtilities.invokeLater(() -> {
            new LoginFrame();
        });
    }
}
