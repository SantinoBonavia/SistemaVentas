package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.DeleteProduct;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ProductDeletionDialogTest {
    @Test
    void namesProductAndOnlyYesDeletesAndRefreshes() throws Exception {
        InMemoryProductRegistrationRepository products = products(); CatalogRefreshCoordinator refresh = new CatalogRefreshCoordinator(); AtomicInteger refreshes = new AtomicInteger(); refresh.subscribe(refreshes::incrementAndGet);
        assertCancelled(products, refresh, ProductDeletionDialog.Decision.NO); assertCancelled(products, refresh, ProductDeletionDialog.Decision.CLOSED);
        CountDownLatch done = new CountDownLatch(1);
        ProductDeletionDialog dialog = new ProductDeletionDialog(new DeleteProduct(products), refresh, products.findProductById(1).orElseThrow(), decision -> ProductDeletionDialog.Decision.YES, done::countDown);
        onEdt(() -> { assertTrue(dialog.prompt().getText().contains("Coffee")); assertEquals("Deletion prompt", dialog.prompt().getAccessibleContext().getAccessibleName()); dialog.deleteButton().doClick(); });
        assertTrue(done.await(2, TimeUnit.SECONDS)); assertTrue(products.findProductById(1).isEmpty()); assertEquals(1, refreshes.get());
    }
    private static void assertCancelled(InMemoryProductRegistrationRepository products, CatalogRefreshCoordinator refresh, ProductDeletionDialog.Decision decision) throws Exception { CountDownLatch done = new CountDownLatch(1); ProductDeletionDialog dialog = new ProductDeletionDialog(new DeleteProduct(products), refresh, products.findProductById(1).orElseThrow(), ignored -> decision, done::countDown); onEdt(() -> dialog.deleteButton().doClick()); assertTrue(done.await(2, TimeUnit.SECONDS)); assertTrue(products.findProductById(1).isPresent()); }
    private static InMemoryProductRegistrationRepository products() throws Exception { InMemoryProductRegistrationRepository products = new InMemoryProductRegistrationRepository(); products.createProductUsingExistingOrNewCategory(new ProductDetails("Coffee", new BigDecimal("2.00"), "Drinks", 1)); return products; }
    private static void onEdt(ThrowingRunnable action) throws Exception { SwingUtilities.invokeAndWait(() -> { try { action.run(); } catch (Exception exception) { throw new RuntimeException(exception); } }); }
    private interface ThrowingRunnable { void run() throws Exception; }
}
