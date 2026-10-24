package com.sistemaventas.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.DeleteProduct;
import com.sistemaventas.inventory.application.DeleteResult;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class DeleteProductTest {

    @Test
    void requestsDeletionForAnExistingIdWithoutChangingCatalogUntilThatExactConfirmationIsConfirmed() throws Exception {
        InMemoryProductRegistrationRepository repository = repositoryWithCoffeeAndTea();
        DeleteProduct deleteProduct = new DeleteProduct(repository);

        DeleteResult.ConfirmationRequired requested = assertInstanceOf(DeleteResult.ConfirmationRequired.class,
                deleteProduct.delete(1));

        assertEquals(List.of("Coffee", "Tea"), repository.listProducts().stream().map(ProductView::name).toList());
        assertInstanceOf(DeleteResult.Deleted.class, deleteProduct.confirm(requested.confirmation().token()));
        assertEquals(List.of(new ProductView(2, "Tea", "Herbal", new BigDecimal("3.25"), 4)), repository.listProducts());
        assertEquals(List.of(), repository.searchProductsByName("coffee"));
    }

    @Test
    void rejectsMissingForgedCancelledAndStaleConfirmationTokensWithoutDeletingData() throws Exception {
        InMemoryProductRegistrationRepository repository = repositoryWithCoffeeAndTea();
        DeleteProduct deleteProduct = new DeleteProduct(repository);

        assertInstanceOf(DeleteResult.Failure.class, deleteProduct.delete(99));
        DeleteResult.ConfirmationRequired requested = assertInstanceOf(DeleteResult.ConfirmationRequired.class,
                deleteProduct.delete(1));
        assertInstanceOf(DeleteResult.Failure.class, deleteProduct.confirm("forged-token"));
        deleteProduct.cancel(requested.confirmation());
        assertInstanceOf(DeleteResult.Failure.class, deleteProduct.confirm(requested.confirmation().token()));
        assertEquals(List.of("Coffee", "Tea"), repository.listProducts().stream().map(ProductView::name).toList());
    }

    @Test
    void confirmsOneTokenOnlyOnceAndConcurrentConfirmationsRemoveOnlyItsSelectedProduct() throws Exception {
        InMemoryProductRegistrationRepository repository = repositoryWithCoffeeAndTea();
        DeleteProduct deleteProduct = new DeleteProduct(repository);
        DeleteResult.ConfirmationRequired requested = assertInstanceOf(DeleteResult.ConfirmationRequired.class,
                deleteProduct.delete(1));
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<DeleteResult> first = executor.submit(() -> deleteProduct.confirm(requested.confirmation().token()));
            Future<DeleteResult> second = executor.submit(() -> deleteProduct.confirm(requested.confirmation().token()));
            assertEquals(1, List.of(first.get(), second.get()).stream().filter(DeleteResult.Deleted.class::isInstance).count());
        } finally {
            executor.shutdownNow();
        }

        assertInstanceOf(DeleteResult.Failure.class, deleteProduct.confirm(requested.confirmation().token()));
        assertEquals(List.of("Tea"), repository.listProducts().stream().map(ProductView::name).toList());
    }

    @Test
    void removesCanonicalNameMembershipRetainsLastProductCategoryAndNeverReusesIds() throws Exception {
        InMemoryProductRegistrationRepository repository = repositoryWithCoffeeAndTea();
        DeleteProduct deleteProduct = new DeleteProduct(repository);
        DeleteResult.ConfirmationRequired requested = assertInstanceOf(DeleteResult.ConfirmationRequired.class,
                deleteProduct.delete(1));

        assertInstanceOf(DeleteResult.Deleted.class, deleteProduct.confirm(requested.confirmation().token()));
        assertEquals(false, repository.productNameExists(" COFFEE "));
        assertEquals(3L, repository.createProductUsingExistingOrNewCategory(details("Coffee", "4.00", "Beverages", 1)));
        assertEquals(List.of("Tea", "Coffee"), repository.listProducts().stream().map(ProductView::name).toList());
    }

    private InMemoryProductRegistrationRepository repositoryWithCoffeeAndTea() throws Exception {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        repository.createProductUsingExistingOrNewCategory(details("Coffee", "12.50", "Beverages", 0));
        repository.createProductUsingExistingOrNewCategory(details("Tea", "3.25", "Herbal", 4));
        return repository;
    }

    private ProductDetails details(String name, String price, String category, int stock) {
        return new ProductDetails(name, new BigDecimal(price), category, stock);
    }
}
