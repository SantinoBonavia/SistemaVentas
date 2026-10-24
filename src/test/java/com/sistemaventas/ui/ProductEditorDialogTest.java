package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.application.UpdateProduct;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ProductEditorDialogTest {
    @Test
    void preloadsUpdatesAndRefreshesOnceWhileDeclinedDuplicateCancelsItsToken() throws Exception {
        InMemoryProductRegistrationRepository products = products();
        CatalogRefreshCoordinator refresh = new CatalogRefreshCoordinator(); AtomicInteger refreshes = new AtomicInteger(); refresh.subscribe(refreshes::incrementAndGet);
        CountDownLatch done = new CountDownLatch(1);
        ProductEditorDialog editor = new ProductEditorDialog(new UpdateProduct(products), refresh, products.findProductById(1).orElseThrow(),
                confirmation -> ProductEditorDialog.Decision.YES, done::countDown);
        onEdt(() -> { assertEquals("Coffee", editor.nameInput().getText()); assertEquals("Drinks", editor.categoryInput().getText()); assertEquals("$2,00", editor.priceInput().getText()); assertEquals("1", editor.stockInput().getText()); assertLabel(editor, "Product name", editor.nameInput()); assertLabel(editor, "Category", editor.categoryInput()); assertLabel(editor, "Price", editor.priceInput()); assertLabel(editor, "Stock", editor.stockInput()); assertEquals(4, java.util.Arrays.stream(editor.getComponents()).filter(JLabel.class::isInstance).map(JLabel.class::cast).filter(label -> label.getLabelFor() != null).count()); editor.nameInput().setText("Tea"); editor.priceInput().setText("$3,50"); editor.saveButton().doClick(); assertFalse(editor.saveButton().isEnabled()); });
        assertTrue(done.await(2, TimeUnit.SECONDS));
        assertEquals(1, products.findProductById(1).orElseThrow().id()); assertEquals("Tea", products.findProductById(1).orElseThrow().name()); assertEquals(1, refreshes.get());

        CountDownLatch accepted = new CountDownLatch(1);
        ProductEditorDialog acceptedDuplicate = new ProductEditorDialog(new UpdateProduct(products), refresh, products.findProductById(1).orElseThrow(),
                confirmation -> ProductEditorDialog.Decision.YES, accepted::countDown);
        onEdt(() -> { acceptedDuplicate.nameInput().setText("Milk"); acceptedDuplicate.saveButton().doClick(); });
        assertTrue(accepted.await(2, TimeUnit.SECONDS)); assertEquals("Milk", products.findProductById(1).orElseThrow().name()); assertEquals(2, refreshes.get());

        assertCancelledDuplicate(products, refresh, ProductEditorDialog.Decision.NO); assertCancelledDuplicate(products, refresh, ProductEditorDialog.Decision.CLOSED);
        assertEquals("Milk", products.findProductById(1).orElseThrow().name()); assertEquals(2, refreshes.get());
    }

    private static InMemoryProductRegistrationRepository products() throws Exception { InMemoryProductRegistrationRepository products = new InMemoryProductRegistrationRepository(); products.createProductUsingExistingOrNewCategory(new ProductDetails("Coffee", new BigDecimal("2.00"), "Drinks", 1)); products.createProductUsingExistingOrNewCategory(new ProductDetails("Milk", new BigDecimal("1.00"), "Drinks", 1)); return products; }
    private static void assertCancelledDuplicate(InMemoryProductRegistrationRepository products, CatalogRefreshCoordinator refresh, ProductEditorDialog.Decision decision) throws Exception { CountDownLatch dismissed = new CountDownLatch(1); ProductEditorDialog dialog = new ProductEditorDialog(new UpdateProduct(products), refresh, products.findProductById(1).orElseThrow(), ignored -> decision, dismissed::countDown); onEdt(() -> { dialog.nameInput().setText("Milk"); dialog.saveButton().doClick(); }); assertTrue(dismissed.await(2, TimeUnit.SECONDS)); assertEquals("Milk", products.findProductById(1).orElseThrow().name()); }
    private static void assertLabel(ProductEditorDialog dialog, String text, Object input) { assertTrue(java.util.Arrays.stream(dialog.getComponents()).filter(JLabel.class::isInstance).map(JLabel.class::cast).anyMatch(label -> text.equals(label.getText()) && label.getLabelFor() == input)); }
    private static void onEdt(ThrowingRunnable action) throws Exception { SwingUtilities.invokeAndWait(() -> { try { action.run(); } catch (Exception exception) { throw new RuntimeException(exception); } }); }
    private interface ThrowingRunnable { void run() throws Exception; }
}
