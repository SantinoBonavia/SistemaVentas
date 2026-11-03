package com.sistemaventas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.application.RegistrationResult;
import com.sistemaventas.persistence.DataStore;
import com.sistemaventas.sales.application.PaymentMethod;
import com.sistemaventas.sales.application.RegisterSale;
import com.sistemaventas.sales.application.SaleLine;
import com.sistemaventas.ui.MainFrame;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ApplicationCompositionTest {

    @Test
    void startsAnEmptyInMemorySessionWithTheFourSectionsAndSellingFirst() throws Exception {
        ApplicationComposition composition = onEdt(ApplicationComposition::create);

        assertTrue(composition.services().browse().list().isEmpty());
        assertEquals(MainFrame.Section.VENDER, composition.mainFrame().current());
        assertEquals(4, MainFrame.Section.values().length);
        assertInstanceOf(RegistrationResult.Created.class, composition.services().register().register(
                new ProductRegistrationCommand("Martillo", "18.900", "Herramientas", "3")));
        assertEquals(List.of("Martillo"), composition.services().browse().list().stream().map(ProductView::name).toList());
    }

    @Test
    void seedsTheHardwareCatalogOnFirstRunAndRestoresSalesAndStockAfterRestart(@TempDir Path directory) throws Exception {
        DataStore store = new DataStore(directory);
        ApplicationComposition first = onEdt(() -> ApplicationComposition.create(
                new ApplicationComposition.NoDialogs(), store, Clock.systemDefaultZone(), true));
        List<ProductView> catalog = first.services().browse().list();
        assertEquals(24, catalog.size());
        assertTrue(catalog.stream().allMatch(product -> product.image().startsWith("art:")));
        ProductView hammer = catalog.stream().filter(product -> product.code().equals("MAR-001")).findFirst().orElseThrow();

        RegisterSale.Result sale = first.services().registerSale().checkout(new RegisterSale.Checkout(
                List.of(new SaleLine(hammer.id(), hammer.code(), hammer.name(), hammer.ivaPercent(), hammer.price(), 2)),
                BigDecimal.ZERO, PaymentMethod.EFECTIVO, null, null));
        assertInstanceOf(RegisterSale.Result.Completed.class, sale);

        ApplicationComposition restarted = onEdt(() -> ApplicationComposition.create(
                new ApplicationComposition.NoDialogs(), store, Clock.systemDefaultZone(), true));
        assertEquals(24, restarted.services().browse().list().size());
        assertEquals(hammer.stock() - 2, restarted.services().browse().list().stream()
                .filter(product -> product.id() == hammer.id()).findFirst().orElseThrow().stock());
        assertEquals(1, restarted.sales().list().size());
        assertEquals(2, restarted.sales().nextNumber());
    }

    @Test
    void inMemorySessionsDoNotShareState() throws Exception {
        ApplicationComposition first = onEdt(ApplicationComposition::create);
        first.services().register().register(new ProductRegistrationCommand("Pinza", "13.700", "Herramientas", "3"));

        ApplicationComposition second = onEdt(ApplicationComposition::create);

        assertEquals(1, first.services().browse().list().size());
        assertFalse(second.services().browse().list().size() > 0);
    }

    static <T> T onEdt(ThrowingSupplier<T> supplier) throws Exception {
        final List<T> result = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> result.add(supplier.get()));
        return result.getFirst();
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get();
    }
}
