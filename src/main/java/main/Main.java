package main;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.JOptionPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame window = new JFrame("Starfall Salvage");
            window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            GamePanel game;
            try { game = new GamePanel(); }
            catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(window, e.getMessage(), "Artwork could not be loaded", JOptionPane.ERROR_MESSAGE);
                window.dispose();
                return;
            }
            window.add(game);
            window.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { game.saveProgress(); }
            });
            window.pack();
            window.setMinimumSize(window.getSize());
            window.setLocationRelativeTo(null);
            window.setVisible(true);
            game.requestFocusInWindow();
        });
    }
}
