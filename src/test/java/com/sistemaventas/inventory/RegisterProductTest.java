package com.sistemaventas.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.sistemaventas.inventory.application.DuplicateConfirmation;
import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.inventory.application.RegistrationResult;
import com.sistemaventas.inventory.port.ProductRegistrationPort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class RegisterProductTest {

    @Test
    void createsTheFirstProductWithTrimmedNamesAndPreservedDecimalScale() {
        FakeProductRegistrationPort port = new FakeProductRegistrationPort();
        RegisterProduct registerProduct = new RegisterProduct(port);

        RegistrationResult result = registerProduct.register(
                new ProductRegistrationCommand("  Coffee  ", " $2.400,00 ", " Beverages ", "0")
        );

        RegistrationResult.Created created = assertInstanceOf(RegistrationResult.Created.class, result);
        assertEquals(1L, created.productId());
        assertEquals("Producto cargado correctamente.", created.message());
        assertEquals(List.of(new CreatedProduct("Coffee", new BigDecimal("2400.00"), "Beverages", 0)), port.createdProducts);
        assertEquals(2, port.createdProducts.get(0).price().scale());
        assertEquals(Set.of("Beverages"), port.categories);
    }

    @Test
    void normalizesPlainWholeAndOneDecimalPricesBeforeDelegatingToThePort() {
        FakeProductRegistrationPort port = new FakeProductRegistrationPort();
        RegisterProduct registerProduct = new RegisterProduct(port);

        assertInstanceOf(RegistrationResult.Created.class,
                registerProduct.register(new ProductRegistrationCommand("Coffee", "12", "Beverages", "0")));
        assertInstanceOf(RegistrationResult.Created.class,
                registerProduct.register(new ProductRegistrationCommand("Tea", "3.5", "Herbal", "4")));

        assertEquals(List.of(
                new CreatedProduct("Coffee", new BigDecimal("12.00"), "Beverages", 0),
                new CreatedProduct("Tea", new BigDecimal("3.50"), "Herbal", 4)
        ), port.createdProducts);
    }

    @Test
    void rejectsInvalidNamesPricesAndStockBeforeAnyPortWork() {
        FakeProductRegistrationPort port = new FakeProductRegistrationPort();
        RegisterProduct registerProduct = new RegisterProduct(port);

        assertValidation(registerProduct.register(new ProductRegistrationCommand(" ", "12.50", "Beverages", "0")), "name", "El nombre del producto es obligatorio.");
        assertValidation(registerProduct.register(new ProductRegistrationCommand("Coffee", "0", "Beverages", "0")), "price", "El precio debe ser un importe positivo con hasta dos decimales.");
        assertValidation(registerProduct.register(new ProductRegistrationCommand("Coffee", "-1.00", "Beverages", "0")), "price", "El precio debe ser un importe positivo con hasta dos decimales.");
        assertValidation(registerProduct.register(new ProductRegistrationCommand("Coffee", "12,345", "Beverages", "0")), "price", "El precio debe ser un importe positivo con hasta dos decimales.");
        assertValidation(registerProduct.register(new ProductRegistrationCommand("Coffee", "1E2", "Beverages", "0")), "price", "El precio debe ser un importe positivo con hasta dos decimales.");
        assertValidation(registerProduct.register(new ProductRegistrationCommand("Coffee", "12.50", " ", "0")), "category", "La categoría es obligatoria.");
        assertValidation(registerProduct.register(new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "-1")), "stock", "El stock debe ser un número entero mayor o igual a cero.");
        assertValidation(registerProduct.register(new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "1.5")), "stock", "El stock debe ser un número entero mayor o igual a cero.");
        assertEquals(List.of(), port.createdProducts);
        assertEquals(Set.of(), port.categories);
        assertEquals(0, port.duplicateChecks);
    }

    @Test
    void delegatesExistingCategoryReuseToTheRegistrationPortContract() {
        FakeProductRegistrationPort port = new FakeProductRegistrationPort();
        port.categories.add("Beverages");
        RegisterProduct registerProduct = new RegisterProduct(port);

        RegistrationResult result = registerProduct.register(
                new ProductRegistrationCommand("Tea", "3.25", "Beverages", "4")
        );

        assertInstanceOf(RegistrationResult.Created.class, result);
        assertEquals(Set.of("Beverages"), port.categories);
        assertEquals(List.of(new CreatedProduct("Tea", new BigDecimal("3.25"), "Beverages", 4)), port.createdProducts);
    }

    @Test
    void cancellingDuplicateConfirmationInvalidatesItsTokenWithoutPersisting() {
        FakeProductRegistrationPort port = new FakeProductRegistrationPort();
        port.knownProductNames.add("Coffee");
        RegisterProduct registerProduct = new RegisterProduct(port);

        RegistrationResult result = registerProduct.register(
                new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "0")
        );

        RegistrationResult.ConfirmationRequired confirmationRequired = assertInstanceOf(
                RegistrationResult.ConfirmationRequired.class,
                result
        );
        registerProduct.cancel(confirmationRequired.confirmation());
        RegistrationResult confirmation = registerProduct.confirm(confirmationRequired.confirmation().token());

        assertNotEquals("Coffee", confirmationRequired.confirmation().token());
        assertFailure(confirmation, "La confirmación de duplicado no es válida o expiró.");
        assertEquals(List.of(), port.createdProducts);
        assertEquals(Set.of(), port.categories);
    }

    @Test
    void createsExactlyOneDuplicateOnlyAfterConfirmingItsOpaqueBoundToken() {
        FakeProductRegistrationPort port = new FakeProductRegistrationPort();
        port.knownProductNames.add("Coffee");
        RegisterProduct registerProduct = new RegisterProduct(port);
        RegistrationResult.ConfirmationRequired pending = assertInstanceOf(
                RegistrationResult.ConfirmationRequired.class,
                registerProduct.register(new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "0"))
        );

        RegistrationResult result = registerProduct.confirm(pending.confirmation().token());

        assertInstanceOf(RegistrationResult.Created.class, result);
        assertEquals(List.of(new CreatedProduct("Coffee", new BigDecimal("12.50"), "Beverages", 0)), port.createdProducts);
        assertEquals(Set.of("Beverages"), port.categories);
    }

    @Test
    void cancellingOneDuplicateDoesNotInvalidateAnotherPendingConfirmation() {
        FakeProductRegistrationPort port = new FakeProductRegistrationPort();
        port.knownProductNames.addAll(Set.of("Coffee", "Tea"));
        RegisterProduct registerProduct = new RegisterProduct(port);
        RegistrationResult.ConfirmationRequired coffee = assertInstanceOf(
                RegistrationResult.ConfirmationRequired.class,
                registerProduct.register(new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "0"))
        );
        RegistrationResult.ConfirmationRequired tea = assertInstanceOf(
                RegistrationResult.ConfirmationRequired.class,
                registerProduct.register(new ProductRegistrationCommand("Tea", "3.25", "Herbal", "4"))
        );

        registerProduct.cancel(coffee.confirmation());

        assertFailure(registerProduct.confirm(coffee.confirmation().token()), "La confirmación de duplicado no es válida o expiró.");
        assertInstanceOf(RegistrationResult.Created.class, registerProduct.confirm(tea.confirmation().token()));
        assertEquals(List.of(new CreatedProduct("Tea", new BigDecimal("3.25"), "Herbal", 4)), port.createdProducts);
        assertEquals(Set.of("Herbal"), port.categories);
    }

    @Test
    void rejectsForgedTokensAndKeepsTokensBoundToTheirValidatedCommands() {
        FakeProductRegistrationPort port = new FakeProductRegistrationPort();
        port.knownProductNames.addAll(Set.of("Coffee", "Tea"));
        RegisterProduct registerProduct = new RegisterProduct(port);
        RegistrationResult.ConfirmationRequired coffee = assertInstanceOf(
                RegistrationResult.ConfirmationRequired.class,
                registerProduct.register(new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "0"))
        );
        registerProduct.register(new ProductRegistrationCommand("Tea", "3.25", "Herbal", "4"));

        RegistrationResult forged = registerProduct.confirm("forged-token");
        RegistrationResult confirmed = registerProduct.confirm(coffee.confirmation().token());

        assertFailure(forged, "La confirmación de duplicado no es válida o expiró.");
        assertInstanceOf(RegistrationResult.Created.class, confirmed);
        assertEquals(List.of(new CreatedProduct("Coffee", new BigDecimal("12.50"), "Beverages", 0)), port.createdProducts);
        assertEquals(Set.of("Beverages"), port.categories);
    }

    @Test
    void mapsPortFailuresToLegibleApplicationResults() {
        FakeProductRegistrationPort port = new FakeProductRegistrationPort();
        port.failureMessage = "connection refused";
        RegisterProduct registerProduct = new RegisterProduct(port);

        RegistrationResult result = registerProduct.register(
                new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "0")
        );

        assertFailure(result, "No se pudo cargar el producto. Intentá de nuevo.");
    }

    @Test
    void confirmsOnePendingDuplicateExactlyOnceWhenWorkersOverlap() throws Exception {
        FakeProductRegistrationPort port = new FakeProductRegistrationPort();
        port.knownProductNames.add("Coffee");
        RegisterProduct registerProduct = new RegisterProduct(port);
        RegistrationResult.ConfirmationRequired pending = assertInstanceOf(
                RegistrationResult.ConfirmationRequired.class,
                registerProduct.register(new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "0"))
        );
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<RegistrationResult>> confirmations = new ArrayList<>();
            for (int index = 0; index < 32; index++) {
                confirmations.add(executor.submit(() -> {
                    start.await();
                    return registerProduct.confirm(pending.confirmation().token());
                }));
            }

            start.countDown();
            int created = 0;
            int rejected = 0;
            for (Future<RegistrationResult> confirmation : confirmations) {
                if (confirmation.get() instanceof RegistrationResult.Created) {
                    created++;
                } else {
                    rejected++;
                }
            }

            assertEquals(1, created);
            assertEquals(31, rejected);
            assertEquals(List.of(new CreatedProduct("Coffee", new BigDecimal("12.50"), "Beverages", 0)), port.createdProducts);
        } finally {
            executor.shutdownNow();
        }
    }

    private void assertValidation(RegistrationResult result, String field, String message) {
        RegistrationResult.ValidationFailed failure = assertInstanceOf(RegistrationResult.ValidationFailed.class, result);
        assertEquals(field, failure.field());
        assertEquals(message, failure.message());
    }

    private void assertFailure(RegistrationResult result, String message) {
        RegistrationResult.Failure failure = assertInstanceOf(RegistrationResult.Failure.class, result);
        assertEquals(message, failure.message());
    }

    private record CreatedProduct(String name, BigDecimal price, String categoryName, int stock) {
    }

    private static final class FakeProductRegistrationPort implements ProductRegistrationPort {
        private final Set<String> knownProductNames = new HashSet<>();
        private final Set<String> categories = new HashSet<>();
        private final List<CreatedProduct> createdProducts = new ArrayList<>();
        private int duplicateChecks;
        private String failureMessage;

        @Override
        public boolean productNameExists(String productName) {
            duplicateChecks++;
            return knownProductNames.contains(productName);
        }

        @Override
        public long createProductUsingExistingOrNewCategory(ProductDetails product) throws ProductRegistrationException {
            if (failureMessage != null) {
                throw new ProductRegistrationException(failureMessage);
            }
            categories.add(product.categoryName());
            createdProducts.add(new CreatedProduct(product.name(), product.price(), product.categoryName(), product.stock()));
            return createdProducts.size();
        }
    }
}
