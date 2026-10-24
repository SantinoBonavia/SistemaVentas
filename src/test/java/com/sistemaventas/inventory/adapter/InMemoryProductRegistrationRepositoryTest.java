package com.sistemaventas.inventory.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.inventory.application.RegistrationResult;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class InMemoryProductRegistrationRepositoryTest {

    @Test
    void resetsProductsCategoriesAndSequencesForEachFreshAdapter() throws Exception {
        InMemoryProductRegistrationRepository populated = new InMemoryProductRegistrationRepository();
        populated.createProductUsingExistingOrNewCategory(details("Coffee", "12.50", "Beverages", 0));

        InMemoryProductRegistrationRepository fresh = new InMemoryProductRegistrationRepository();

        assertEquals(List.of(), fresh.products());
        assertEquals(List.of(), fresh.categories());
        assertEquals(1L, fresh.createProductUsingExistingOrNewCategory(details("Tea", "3.25", "Herbal", 4)));
        assertEquals(List.of(new InMemoryProductRegistrationRepository.CategorySnapshot(1L, "Herbal")), fresh.categories());
    }

    @Test
    void allocatesIndependentDeterministicProductAndCategoryIdentifiers() throws Exception {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();

        assertEquals(1L, repository.createProductUsingExistingOrNewCategory(details("Coffee", "12.50", "Beverages", 0)));
        assertEquals(2L, repository.createProductUsingExistingOrNewCategory(details("Tea", "3.25", "Herbal", 4)));
        assertEquals(List.of(
                new InMemoryProductRegistrationRepository.CategorySnapshot(1L, "Beverages"),
                new InMemoryProductRegistrationRepository.CategorySnapshot(2L, "Herbal")
        ), repository.categories());
        assertEquals(List.of(
                new InMemoryProductRegistrationRepository.ProductSnapshot(1L, "Coffee", new BigDecimal("12.50"), 1L, 0),
                new InMemoryProductRegistrationRepository.ProductSnapshot(2L, "Tea", new BigDecimal("3.25"), 2L, 4)
        ), repository.products());
    }

    @Test
    void reusesTheFirstCanonicalCategoryDisplayNameAndRetainsExactValues() throws Exception {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        repository.createProductUsingExistingOrNewCategory(details("Coffee", "12.50", " Beverages ", 0));

        repository.createProductUsingExistingOrNewCategory(details("Tea", "3.25", "beverages", 4));

        assertEquals(List.of(new InMemoryProductRegistrationRepository.CategorySnapshot(1L, "Beverages")), repository.categories());
        assertEquals(new InMemoryProductRegistrationRepository.ProductSnapshot(1L, "Coffee", new BigDecimal("12.50"), 1L, 0), repository.products().get(0));
        assertEquals(2, repository.products().get(0).price().scale());
        assertEquals(1L, repository.products().get(1).categoryId());
    }

    @Test
    void checksProductDuplicatesCaseInsensitivelyUsingTrimmedUiNames() throws Exception {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        repository.createProductUsingExistingOrNewCategory(details("Coffee", "12.50", "Beverages", 0));

        assertTrue(repository.productNameExists(" coffee "));
        assertTrue(repository.productNameExists("COFFEE"));
        assertFalse(repository.productNameExists("Tea"));
    }

    @Test
    void listsDefensiveProductViewsAndSearchesNamesCaseInsensitivelyInInsertionOrder() throws Exception {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        repository.createProductUsingExistingOrNewCategory(details("Coffee", "12.50", "Beverages", 0));
        repository.createProductUsingExistingOrNewCategory(details("Iced coffee", "3.25", "Beverages", 4));
        repository.createProductUsingExistingOrNewCategory(details("Tea", "2.00", "Herbal", 1));

        List<ProductView> all = repository.listProducts();
        assertEquals(List.of(
                new ProductView(1L, "Coffee", "Beverages", new BigDecimal("12.50"), 0),
                new ProductView(2L, "Iced coffee", "Beverages", new BigDecimal("3.25"), 4),
                new ProductView(3L, "Tea", "Herbal", new BigDecimal("2.00"), 1)
        ), all);
        assertEquals(List.of(all.get(0), all.get(1)), repository.searchProductsByName(" COFFEE "));
        assertEquals(List.of(), repository.searchProductsByName("milk"));
        assertThrows(UnsupportedOperationException.class, () -> all.add(all.get(0)));
    }

    @Test
    void invalidOrCancelledRequestsDoNotConsumeProductOrCategoryIdentifiers() {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        RegisterProduct registerProduct = new RegisterProduct(repository);

        assertInstanceOf(RegistrationResult.ValidationFailed.class,
                registerProduct.register(new ProductRegistrationCommand("Coffee", "0", "Beverages", "0")));
        assertInstanceOf(RegistrationResult.Created.class,
                registerProduct.register(new ProductRegistrationCommand("Coffee", "12.50", "Beverages", "0")));
        RegistrationResult.ConfirmationRequired duplicate = assertInstanceOf(RegistrationResult.ConfirmationRequired.class,
                registerProduct.register(new ProductRegistrationCommand("coffee", "12.50", "Beverages", "0")));

        registerProduct.cancel(duplicate.confirmation());

        assertInstanceOf(RegistrationResult.Created.class,
                registerProduct.register(new ProductRegistrationCommand("Tea", "3.25", "Herbal", "4")));
        assertEquals(List.of(
                new InMemoryProductRegistrationRepository.ProductSnapshot(1L, "Coffee", new BigDecimal("12.50"), 1L, 0),
                new InMemoryProductRegistrationRepository.ProductSnapshot(2L, "Tea", new BigDecimal("3.25"), 2L, 4)
        ), repository.products());
    }

    @Test
    void createsProductsAndOneCanonicalCategoryAtomicallyForOverlappingWorkers() throws Exception {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Long>> submissions = new ArrayList<>();
            for (int index = 0; index < 40; index++) {
                int worker = index;
                submissions.add(() -> repository.createProductUsingExistingOrNewCategory(
                        details("Product " + worker, "12.50", "Beverages", worker)
                ));
            }

            List<Future<Long>> results = executor.invokeAll(submissions);
            List<Long> identifiers = new ArrayList<>();
            for (Future<Long> result : results) {
                identifiers.add(result.get());
            }

            assertEquals(40, identifiers.stream().distinct().count());
            assertEquals(40, repository.products().size());
            assertEquals(List.of(new InMemoryProductRegistrationRepository.CategorySnapshot(1L, "Beverages")), repository.categories());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void updatesPreserveIdentityInsertionOrderAndBothOldAndNewCategoriesUnderOverlappingWorkers() throws Exception {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        for (int index = 0; index < 20; index++) {
            repository.createProductUsingExistingOrNewCategory(details("Product " + index, "12.50", "Old", index));
        }
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> updates = new ArrayList<>();
            for (int index = 1; index <= 20; index++) {
                long id = index;
                updates.add(() -> {
                    repository.updateProduct(id, details("Updated " + id, "4.5", "New", 7));
                    return null;
                });
            }
            for (Future<Void> update : executor.invokeAll(updates)) {
                update.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(20, repository.products().size());
        assertEquals(List.of(
                new InMemoryProductRegistrationRepository.CategorySnapshot(1L, "Old"),
                new InMemoryProductRegistrationRepository.CategorySnapshot(2L, "New")
        ), repository.categories());
        assertEquals(List.of(1L, 2L, 3L), repository.products().subList(0, 3).stream().map(product -> product.id()).toList());
        assertTrue(repository.products().stream().allMatch(product -> product.price().equals(new BigDecimal("4.5"))));
    }

    @Test
    void deletesOnlyTheRequestedProductAtomicallyWhileRetainingCategoriesAndAdvancingIds() throws Exception {
        InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
        repository.createProductUsingExistingOrNewCategory(details("Coffee", "12.50", "Beverages", 0));
        repository.createProductUsingExistingOrNewCategory(details("Tea", "3.25", "Herbal", 4));

        repository.deleteProduct(1);

        assertFalse(repository.productNameExists("coffee"));
        assertEquals(List.of(new InMemoryProductRegistrationRepository.CategorySnapshot(1L, "Beverages"),
                new InMemoryProductRegistrationRepository.CategorySnapshot(2L, "Herbal")), repository.categories());
        assertEquals(3L, repository.createProductUsingExistingOrNewCategory(details("Coffee", "4.00", "Beverages", 1)));
        assertEquals(List.of(2L, 3L), repository.products().stream().map(product -> product.id()).toList());
        assertEquals(1L, repository.products().get(1).categoryId());
    }

    private ProductDetails details(String name, String price, String category, int stock) {
        return new ProductDetails(name, new BigDecimal(price), category, stock);
    }
}
