package com.sistemaventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.RegistrationResult;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.ui.ProductDeletionDialog;
import com.sistemaventas.ui.ProductEditorDialog;
import com.sistemaventas.ui.SettingsDialog;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ApplicationCompositionTest {

    @Test
    void createsTwoHeadlessSafeTabsOverOneFreshInMemoryProductSession() throws Exception {
        ApplicationComposition composition = onEdt(Main::createComposition);

        assertEquals(2, composition.mainFrame().tabs().getTabCount());
        assertEquals("Load Product", composition.mainFrame().tabs().getTitleAt(0));
        assertEquals("Products", composition.mainFrame().tabs().getTitleAt(1));
        assertInstanceOf(RegistrationResult.Created.class, composition.registerProduct().register(
                new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "3")
        ));
        assertEquals(List.of("Coffee"), composition.browseProducts().list().stream().map(ProductView::name).toList());
    }

    @Test
    void wiresCatalogAndSettingsActionsToHeadlessControllersWithoutDatabaseStartup() throws Exception {
        RecordingWindows windows = new RecordingWindows();
        ApplicationComposition composition = onEdt(() -> ApplicationComposition.create(windows));
        composition.registerProduct().register(new ProductRegistrationCommand("Tea", "4.20", "Beverages", "2"));
        ProductView tea = composition.browseProducts().list().getFirst();

        composition.openEditor(tea);
        composition.openDeletion(tea);
        composition.openSettings();

        assertEquals(3, windows.panels.size());
        assertInstanceOf(ProductEditorDialog.class, windows.panels.get(0));
        assertInstanceOf(ProductDeletionDialog.class, windows.panels.get(1));
        assertInstanceOf(SettingsDialog.class, windows.panels.get(2));
        assertEquals(Optional.of(Currency.getInstance("ARS")), composition.currencySettings().load());
    }

    @Test
    void startsASeparateEmptySessionForEveryCompositionRoot() throws Exception {
        ApplicationComposition first = onEdt(ApplicationComposition::create);
        first.registerProduct().register(new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "3"));

        ApplicationComposition restarted = onEdt(ApplicationComposition::create);

        assertEquals(1, first.browseProducts().list().size());
        assertTrue(restarted.browseProducts().list().isEmpty());
        assertEquals(Optional.of(Currency.getInstance("ARS")), restarted.currencySettings().load());
    }

    private static <T> T onEdt(ThrowingSupplier<T> supplier) throws Exception {
        final List<T> result = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> result.add(supplier.get()));
        return result.getFirst();
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get();
    }

    private static final class RecordingWindows implements ApplicationComposition.WindowHost {
        private final List<JPanel> panels = new ArrayList<>();

        @Override
        public void show(String title, Function<Runnable, JPanel> content) {
            panels.add(content.apply(() -> { }));
        }
    }
}
