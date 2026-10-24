package com.sistemaventas.ui;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.SwingUtilities;

/** UI-only refresh event boundary; subscribers do not know one another's panels. */
public final class CatalogRefreshCoordinator {
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    public AutoCloseable subscribe(Runnable listener) {
        Runnable registered = Objects.requireNonNull(listener);
        listeners.add(registered);
        return () -> listeners.remove(registered);
    }

    public void publish() {
        Runnable notifyListeners = () -> listeners.forEach(listener -> {
            try {
                listener.run();
            } catch (RuntimeException ignored) {
                // A disposed view must not prevent another catalog listener from refreshing.
            }
        });
        if (SwingUtilities.isEventDispatchThread()) notifyListeners.run();
        else SwingUtilities.invokeLater(notifyListeners);
    }
}
