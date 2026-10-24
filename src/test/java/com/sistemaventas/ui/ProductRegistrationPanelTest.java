package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.inventory.application.RegistrationResult;
import com.sistemaventas.inventory.port.ProductRegistrationPort;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductRegistrationException;
import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ProductRegistrationPanelTest {

    @Test
    void exposesLabeledAccessibleInputsAndKeepsInitiatingControlsBusyUntilBackgroundCreationCompletes() throws Exception {
        BlockingPort port = new BlockingPort(false);
        Fixture fixture = new Fixture(new RegisterProduct(port), confirmation -> true);

        onEdt(() -> {
            fixture.panel.nameInput().setText("Coffee");
            fixture.panel.categoryInput().setText("Beverages");
            fixture.panel.priceInput().setText("$2.400,00");
            fixture.panel.stockInput().setText("0");
            fixture.panel.submitButton().doClick();
            assertFalse(fixture.panel.submitButton().isEnabled());
            assertFalse(fixture.panel.nameInput().isEnabled());
            assertEquals("Product name", fixture.panel.nameInput().getAccessibleContext().getAccessibleName());
            assertEquals("Category", fixture.panel.categoryInput().getAccessibleContext().getAccessibleName());
            assertEquals("Price", fixture.panel.priceInput().getAccessibleContext().getAccessibleName());
            assertEquals("Stock", fixture.panel.stockInput().getAccessibleContext().getAccessibleName());
        });
        assertTrue(port.started.await(2, TimeUnit.SECONDS));
        assertFalse(port.calledOnEdt.get());

        port.release.countDown();
        assertTrue(fixture.completed.await(2, TimeUnit.SECONDS));
        onEdt(() -> {
            assertTrue(fixture.panel.submitButton().isEnabled());
            assertEquals("Product registered successfully.", fixture.panel.feedback().getText());
            assertEquals("", fixture.panel.nameInput().getText());
            assertEquals("Beverages", fixture.panel.categoryInput().getText());
            assertEquals("", fixture.panel.priceInput().getText());
            assertEquals("", fixture.panel.stockInput().getText());
        });
        assertEquals(1, fixture.refreshes.get());
        assertEquals(new BigDecimal("2400.00"), port.createdProduct.get().price());
        assertTrue(fixture.completedOnEdt.get());
    }

    @Test
    void showsFieldSpecificValidationFeedbackWithoutPublishingARefresh() throws Exception {
        Fixture fixture = new Fixture(new RegisterProduct(new BlockingPort(false)), confirmation -> true);

        onEdt(() -> {
            fixture.panel.nameInput().setText(" ");
            fixture.panel.categoryInput().setText("Beverages");
            fixture.panel.priceInput().setText("12.50");
            fixture.panel.stockInput().setText("0");
            fixture.panel.submitButton().doClick();
        });
        assertTrue(fixture.completed.await(2, TimeUnit.SECONDS));

        onEdt(() -> {
            assertEquals("Product name is required.", fixture.panel.nameFeedback().getText());
            assertEquals("Product name is required.", fixture.panel.feedback().getText());
            assertTrue(fixture.panel.submitButton().isEnabled());
        });
        assertEquals(0, fixture.refreshes.get());
    }

    @Test
    void identifiesTheInvalidPriceFieldWithLegibleFeedback() throws Exception {
        Fixture fixture = new Fixture(new RegisterProduct(new BlockingPort(false)), confirmation -> true);

        onEdt(() -> {
            fixture.panel.nameInput().setText("Coffee");
            fixture.panel.categoryInput().setText("Beverages");
            fixture.panel.priceInput().setText("12.555");
            fixture.panel.stockInput().setText("0");
            fixture.panel.submitButton().doClick();
        });
        assertTrue(fixture.completed.await(2, TimeUnit.SECONDS));

        onEdt(() -> assertEquals("Price must be a positive amount with at most two decimal places.",
                fixture.panel.priceInput().getToolTipText()));
        assertEquals(0, fixture.refreshes.get());
    }

    @Test
    void confirmsOrCancelsTypedDuplicatesWithoutExtraRefreshes() throws Exception {
        BlockingPort port = new BlockingPort(true);
        port.duplicate = true;
        Fixture accepted = new Fixture(new RegisterProduct(port), confirmation -> true);

        submitValid(accepted.panel);
        assertTrue(accepted.completed.await(2, TimeUnit.SECONDS));
        assertEquals(1, port.created.get());
        assertEquals(1, accepted.refreshes.get());

        BlockingPort cancelledPort = new BlockingPort(true);
        cancelledPort.duplicate = true;
        AtomicReference<String> cancelledToken = new AtomicReference<>();
        RegisterProduct cancelledUseCase = new RegisterProduct(cancelledPort);
        Fixture cancelled = new Fixture(cancelledUseCase, confirmation -> {
            cancelledToken.set(confirmation.token());
            return false;
        });
        submitValid(cancelled.panel);
        assertTrue(cancelled.completed.await(2, TimeUnit.SECONDS));

        assertEquals(0, cancelledPort.created.get());
        assertEquals(0, cancelled.refreshes.get());
        assertInstanceOf(RegistrationResult.Failure.class, cancelledUseCase.confirm(cancelledToken.get()));
    }

    private static void submitValid(ProductRegistrationPanel panel) throws Exception {
        onEdt(() -> {
            panel.nameInput().setText("Coffee");
            panel.categoryInput().setText("Beverages");
            panel.priceInput().setText("12.50");
            panel.stockInput().setText("0");
            panel.submitButton().doClick();
        });
    }

    private static void onEdt(ThrowingRunnable runnable) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                runnable.run();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private static final class Fixture {
        private final CountDownLatch completed = new CountDownLatch(1);
        private final AtomicInteger refreshes = new AtomicInteger();
        private final AtomicBoolean completedOnEdt = new AtomicBoolean();
        private final ProductRegistrationPanel panel;

        private Fixture(RegisterProduct registerProduct, ProductRegistrationPanel.DuplicateConfirmationBoundary confirmation) {
            CatalogRefreshCoordinator coordinator = new CatalogRefreshCoordinator();
            panel = new ProductRegistrationPanel(registerProduct, coordinator, confirmation, () -> {
                completedOnEdt.set(SwingUtilities.isEventDispatchThread());
                completed.countDown();
            });
            coordinator.subscribe(refreshes::incrementAndGet);
        }
    }

    private static final class BlockingPort implements ProductRegistrationPort {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release;
        private final AtomicInteger created = new AtomicInteger();
        private final AtomicReference<ProductDetails> createdProduct = new AtomicReference<>();
        private final AtomicBoolean calledOnEdt = new AtomicBoolean();
        private volatile boolean duplicate;

        private BlockingPort(boolean duplicate) {
            this.duplicate = duplicate;
            this.release = new CountDownLatch(duplicate ? 0 : 1);
        }

        @Override
        public boolean productNameExists(String productName) {
            return duplicate;
        }

        @Override
        public long createProductUsingExistingOrNewCategory(ProductDetails product) throws ProductRegistrationException {
            calledOnEdt.set(SwingUtilities.isEventDispatchThread());
            started.countDown();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) throw new ProductRegistrationException("Timed out");
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new ProductRegistrationException("Interrupted");
            }
            createdProduct.set(product);
            return created.incrementAndGet();
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
