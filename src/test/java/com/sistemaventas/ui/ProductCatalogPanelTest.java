package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.application.BrowseProducts;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.port.ProductCatalogPort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ProductCatalogPanelTest {
    @Test
    void showsRequiredColumnsAndInsertionOrderWithCurrentCurrency() throws Exception {
        Fixture fixture = new Fixture(List.of(product(1, "Coffee"), product(2, "Tea")));
        fixture.awaitLoad();
        onEdt(() -> {
            assertArrayEquals(new String[] {"ID", "Name", "Category", "Price", "Stock"}, fixture.panel.columns());
            assertEquals(List.of("1", "2"), fixture.panel.ids());
            assertEquals("$2.400,00", fixture.panel.priceAt(0));
        });
        fixture.currency.set(Optional.of(Currency.getInstance("EUR"))); fixture.expectLoad(); fixture.refresh.publish();
        fixture.awaitLoad();
        onEdt(() -> assertEquals("EUR2.400,00", fixture.panel.priceAt(0)));
    }

    @Test
    void clearlyDistinguishesEmptyAndNoResultsAndFiltersOnlyTrimmedCaseInsensitiveNames() throws Exception {
        Fixture empty = new Fixture(List.of());
        empty.awaitLoad();
        onEdt(() -> assertEquals("No products have been added yet.", empty.panel.status().getText()));
        Fixture fixture = new Fixture(List.of(product(1, "Coffee"), product(2, "Office Chair")));
        fixture.awaitLoad();
        onEdt(() -> { fixture.panel.filterInput().setText(" COF "); fixture.expectLoad(); fixture.panel.applyFilter(); }); fixture.awaitLoad();
        onEdt(() -> assertEquals(List.of("1"), fixture.panel.ids()));
        onEdt(() -> { fixture.panel.filterInput().setText("Category"); fixture.expectLoad(); fixture.panel.applyFilter(); }); fixture.awaitLoad();
        onEdt(() -> assertEquals("No products match your search.", fixture.panel.status().getText()));
    }

    @Test
    void loadsOffEdtDisablesControlsAndKeepsLatestRequestWhenOlderWorkerFinishesLast() throws Exception {
        Fixture fixture = new Fixture(List.of(product(1, "Coffee"))); fixture.awaitLoad();
        onEdt(() -> { fixture.port.blockNext(); fixture.expectLoad(); fixture.panel.refresh(); });
        assertTrue(fixture.port.started.await(2, TimeUnit.SECONDS));
        onEdt(() -> { assertFalse(fixture.panel.filterInput().isEnabled()); assertFalse(fixture.panel.refreshButton().isEnabled()); });
        fixture.port.products = List.of(product(2, "Tea")); onEdt(() -> { fixture.expectLoad(); fixture.panel.refresh(); }); fixture.awaitLoad();
        fixture.expectLoad(); fixture.port.release.countDown(); fixture.awaitLoad();
        onEdt(() -> assertEquals(List.of("2"), fixture.panel.ids()));
        assertFalse(fixture.port.calledOnEdt.get());
    }

    @Test
    void refreshSubscriptionDelegatesSelectedRowsToTypedActionsAndShowsLegibleErrors() throws Exception {
        Fixture fixture = new Fixture(List.of(product(1, "Coffee")));
        fixture.awaitLoad(); fixture.expectLoad(); fixture.refresh.publish(); fixture.awaitLoad();
        onEdt(() -> { fixture.panel.selectRow(0); fixture.panel.editButton().doClick(); fixture.panel.deleteButton().doClick(); });
        assertEquals(List.of(1L), fixture.edited); assertEquals(List.of(1L), fixture.deleted);
        fixture.port.fail = true; onEdt(() -> { fixture.expectLoad(); fixture.panel.refresh(); }); fixture.awaitLoad();
        onEdt(() -> assertEquals("Products could not be loaded. Please try again.", fixture.panel.status().getText()));
    }

    private static ProductView product(long id, String name) { return new ProductView(id, name, "Category", new BigDecimal("2400.00"), 3); }
    private static void onEdt(ThrowingRunnable action) throws Exception { SwingUtilities.invokeAndWait(() -> { try { action.run(); } catch (Exception exception) { throw new RuntimeException(exception); } }); }
    @FunctionalInterface private interface ThrowingRunnable { void run() throws Exception; }

    private static final class Fixture {
        final CatalogRefreshCoordinator refresh = new CatalogRefreshCoordinator(); final FakePort port; final AtomicReference<Optional<Currency>> currency = new AtomicReference<>(Optional.of(Currency.getInstance("ARS")));
        final List<Long> edited = new ArrayList<>(), deleted = new ArrayList<>(); volatile CountDownLatch loaded = new CountDownLatch(1); ProductCatalogPanel panel;
        Fixture(List<ProductView> products) throws Exception { port = new FakePort(products); onEdt(() -> panel = new ProductCatalogPanel(new BrowseProducts(port), refresh, currency::get, product -> edited.add(product.id()), product -> deleted.add(product.id()), () -> loaded.countDown())); }
        void expectLoad() { loaded = new CountDownLatch(1); }
        void awaitLoad() throws Exception { assertTrue(loaded.await(2, TimeUnit.SECONDS)); }
    }

    private static final class FakePort implements ProductCatalogPort {
        volatile List<ProductView> products; volatile boolean fail; final AtomicBoolean calledOnEdt = new AtomicBoolean(); final CountDownLatch started = new CountDownLatch(1), release = new CountDownLatch(1); volatile boolean block;
        FakePort(List<ProductView> products) { this.products = products; }
        void blockNext() { block = true; }
        @Override public List<ProductView> listProducts() { calledOnEdt.set(SwingUtilities.isEventDispatchThread()); if (block) { block = false; started.countDown(); try { release.await(2, TimeUnit.SECONDS); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); } } if (fail) throw new IllegalStateException(); return List.copyOf(products); }
        @Override public List<ProductView> searchProductsByName(String ignored) { return listProducts(); }
    }
}
