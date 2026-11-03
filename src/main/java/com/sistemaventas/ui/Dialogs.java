package com.sistemaventas.ui;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.JComponent;

/** Ventanas modales. La interfaz permite reemplazarlas en pruebas sin pantalla. */
public interface Dialogs {

    /** Muestra {@code content}; el Runnable que recibe cierra la ventana. */
    void show(String title, Function<Runnable, JComponent> content);

    boolean confirm(String title, String message, String confirmLabel);

    void message(String title, String message);

    Optional<Path> choosePhoto();
}
