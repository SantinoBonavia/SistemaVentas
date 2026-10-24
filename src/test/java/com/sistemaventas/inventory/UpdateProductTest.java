package com.sistemaventas.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.RegistrationResult;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.inventory.application.UpdateProduct;
import com.sistemaventas.inventory.application.UpdateProductCommand;
import com.sistemaventas.inventory.application.UpdateResult;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class UpdateProductTest {

    @Test
    void updatesStableIdWithSharedValidationExactPriceAndRetainedCategories() {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        register(repository, "Coffee", "12.50", "Beverages", "0");
        register(repository, "Tea", "3.25", "Food", "4");
        UpdateProduct updateProduct = new UpdateProduct(repository);

        UpdateResult result = updateProduct.update(new UpdateProductCommand(1, "Espresso", "$2.400,00", "Food", "3"));

        assertInstanceOf(UpdateResult.Updated.class, result);
        assertEquals(List.of(
                new com.sistemaventas.inventory.application.ProductView(1, "Espresso", "Food", new BigDecimal("2400.00"), 3),
                new com.sistemaventas.inventory.application.ProductView(2, "Tea", "Food", new BigDecimal("3.25"), 4)
        ), repository.listProducts());
    }

    @Test
    void rejectsMissingIdsAndCreationInvalidInputsWithoutChangingData() {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        register(repository, "Coffee", "12.50", "Beverages", "0");
        UpdateProduct updateProduct = new UpdateProduct(repository);

        assertInstanceOf(UpdateResult.Failure.class,
                updateProduct.update(new UpdateProductCommand(99, "Tea", "3.25", "Herbal", "1")));
        assertInstanceOf(UpdateResult.ValidationFailed.class,
                updateProduct.update(new UpdateProductCommand(1, "Tea", "3.257", "Herbal", "1")));
        assertEquals(List.of(new com.sistemaventas.inventory.application.ProductView(1, "Coffee", "Beverages", new BigDecimal("12.50"), 0)), repository.listProducts());
    }

    @Test
    void unchangedNameDoesNotWarnButRenamingToAnotherCanonicalNameRequiresOneShotConfirmation() {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        register(repository, "Coffee", "12.50", "Beverages", "0");
        register(repository, "Tea", "3.25", "Herbal", "4");
        UpdateProduct updateProduct = new UpdateProduct(repository);

        assertInstanceOf(UpdateResult.Updated.class,
                updateProduct.update(new UpdateProductCommand(1, " coffee ", "12.50", "Beverages", "1")));
        UpdateResult.ConfirmationRequired warning = assertInstanceOf(UpdateResult.ConfirmationRequired.class,
                updateProduct.update(new UpdateProductCommand(2, "COFFEE", "4", "Food", "2")));
        assertInstanceOf(UpdateResult.Updated.class, updateProduct.confirm(warning.confirmation().token()));
        assertInstanceOf(UpdateResult.Failure.class, updateProduct.confirm(warning.confirmation().token()));
        assertInstanceOf(UpdateResult.Failure.class, updateProduct.confirm("forged-token"));
        assertEquals("COFFEE", repository.listProducts().get(1).name());
    }

    @Test
    void cancellingOrConfirmingOnePendingEditOnlyAffectsThatBoundEdit() {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        register(repository, "Coffee", "12.50", "Beverages", "0");
        register(repository, "Tea", "3.25", "Herbal", "4");
        register(repository, "Milk", "2.00", "Food", "1");
        UpdateProduct updateProduct = new UpdateProduct(repository);
        UpdateResult.ConfirmationRequired teaWarning = assertInstanceOf(UpdateResult.ConfirmationRequired.class,
                updateProduct.update(new UpdateProductCommand(2, "Coffee", "3.25", "Herbal", "4")));
        UpdateResult.ConfirmationRequired milkWarning = assertInstanceOf(UpdateResult.ConfirmationRequired.class,
                updateProduct.update(new UpdateProductCommand(3, "Coffee", "2.00", "Food", "1")));

        updateProduct.cancel(teaWarning.confirmation());

        assertInstanceOf(UpdateResult.Failure.class, updateProduct.confirm(teaWarning.confirmation().token()));
        assertInstanceOf(UpdateResult.Updated.class, updateProduct.confirm(milkWarning.confirmation().token()));
        assertEquals(List.of("Coffee", "Tea", "Coffee"), repository.listProducts().stream().map(product -> product.name()).toList());
    }

    @Test
    void overlappingConfirmationWorkersApplyOnePendingEditExactlyOnce() throws Exception {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        register(repository, "Coffee", "12.50", "Beverages", "0");
        register(repository, "Tea", "3.25", "Herbal", "4");
        UpdateProduct updateProduct = new UpdateProduct(repository);
        UpdateResult.ConfirmationRequired warning = assertInstanceOf(UpdateResult.ConfirmationRequired.class,
                updateProduct.update(new UpdateProductCommand(2, "Coffee", "4", "Food", "2")));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<UpdateResult> first = executor.submit(() -> updateProduct.confirm(warning.confirmation().token()));
            Future<UpdateResult> second = executor.submit(() -> updateProduct.confirm(warning.confirmation().token()));
            assertEquals(1, List.of(first.get(), second.get()).stream().filter(UpdateResult.Updated.class::isInstance).count());
        } finally {
            executor.shutdownNow();
        }
        assertEquals(new BigDecimal("4.00"), repository.listProducts().get(1).price());
    }

    private void register(InMemoryProductRegistrationRepository repository, String name, String price, String category, String stock) {
        assertInstanceOf(RegistrationResult.Created.class,
                new RegisterProduct(repository).register(new ProductRegistrationCommand(name, price, category, stock)));
    }
}
