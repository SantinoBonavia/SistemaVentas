package com.sistemaventas;

import com.sistemaventas.persistence.DataStore;
import com.sistemaventas.ui.SwingDialogs;
import com.sistemaventas.ui.Theme;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.time.Clock;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    private static void start() {
        Theme.install();
        JFrame frame = new JFrame("Sistema de Ventas — Ferretería");
        ApplicationComposition application;
        try {
            application = ApplicationComposition.create(new SwingDialogs(() -> frame), DataStore.defaultLocation(),
                    Clock.systemDefaultZone(), true);
        } catch (IllegalStateException exception) {
            JOptionPane.showMessageDialog(null, exception.getMessage(), "Sistema de Ventas", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImage(com.sistemaventas.ui.ToolArt.image("llave", 64));
        frame.setContentPane(application.mainFrame().component());
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setMinimumSize(new Dimension(1180, 720));
        frame.setSize(Math.min(1500, screen.width - 40), Math.min(900, screen.height - 60));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
