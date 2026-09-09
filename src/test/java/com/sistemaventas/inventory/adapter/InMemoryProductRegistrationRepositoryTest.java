package com.sistemaventas.inventory.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.application.ProductRegistrationCommand;
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
                        details("Product " + worker, "12.50", worker % 2 == 0 ? "Beverages" : " beverages ", worker)
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

    private ProductDetails details(String name, String price, String category, int stock) {
        return new ProductDetails(name, new BigDecimal(price), category, stock);
    }
}
