package com.sistemaventas.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Implementación de escritorio de {@link Dialogs}. */
public final class SwingDialogs implements Dialogs {
    private final Supplier<Window> owner;
    private Path lastDirectory;

    public SwingDialogs(Supplier<Window> owner) {
        this.owner = owner;
    }

    @Override
    public void show(String title, Function<Runnable, JComponent> content) {
        JDialog dialog = new JDialog(owner.get(), title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());
        dialog.add(content.apply(dialog::dispose), BorderLayout.CENTER);
        dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(owner.get());
        dialog.setVisible(true);
    }

    @Override
    public boolean confirm(String title, String message, String confirmLabel) {
        Object[] options = {confirmLabel, "Cancelar"};
        int answer = JOptionPane.showOptionDialog(parent(), message, title, JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE, null, options, options[1]);
        return answer == 0;
    }

    @Override
    public void message(String title, String message) {
        JOptionPane.showMessageDialog(parent(), message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public Optional<Path> choosePhoto() {
        JFileChooser chooser = new JFileChooser(lastDirectory == null ? null : lastDirectory.toFile());
        chooser.setDialogTitle("Elegir foto del producto");
        chooser.setFileFilter(new FileNameExtensionFilter("Imágenes (JPG, PNG, GIF, BMP)", "jpg", "jpeg", "png", "gif", "bmp"));
        if (chooser.showOpenDialog(parent()) != JFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null) {
            return Optional.empty();
        }
        Path selected = chooser.getSelectedFile().toPath();
        lastDirectory = selected.getParent();
        return Optional.of(selected);
    }

    private Component parent() {
        Window focused = javax.swing.FocusManager.getCurrentManager().getActiveWindow();
        return focused != null ? focused : owner.get();
    }
}
