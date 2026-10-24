package com.sistemaventas;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Window;
import java.util.function.Function;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::start);
    }

    static ApplicationComposition createComposition() {
        return ApplicationComposition.create(Main::showDialog);
    }

    private static void start() {
        ApplicationComposition application = createComposition();
        JFrame frame = new JFrame("SistemaVentas - Product Inventory");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(application.mainFrame().component());
        frame.setSize(960, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static void showDialog(String title, Function<Runnable, JPanel> content) {
        JDialog dialog = new JDialog((Window) null, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(content.apply(dialog::dispose), BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }
}
