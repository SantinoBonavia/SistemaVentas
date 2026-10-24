package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class MainFrameTest {

    @Test
    void createsAHeadlessSafeShellWithInjectedLoadAndProductTabsAndSettingsBoundary() throws Exception {
        JPanel loading = new JPanel();
        JPanel products = new JPanel();
        AtomicBoolean settingsOpened = new AtomicBoolean();

        onEdt(() -> {
            MainFrame shell = new MainFrame(loading, products, () -> settingsOpened.set(true));

            assertEquals(2, shell.tabs().getTabCount());
            assertEquals("Load Product", shell.tabs().getTitleAt(0));
            assertEquals("Products", shell.tabs().getTitleAt(1));
            assertSame(loading, shell.tabs().getComponentAt(0));
            assertSame(products, shell.tabs().getComponentAt(1));
            assertTrue(shell.component().isFocusable());
            ((JButton) shell.component().getComponent(0)).doClick();
            assertTrue(settingsOpened.get());
        });
    }

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }
}
