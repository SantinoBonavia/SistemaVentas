package com.sistemaventas.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sistemaventas.inventory.application.BrowseProducts;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.port.ProductCatalogPort;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class BrowseProductsTest {

    @Test
    void listsProductsInInsertionOrderUsingAnImmutableResult() {
        ProductView coffee = product(1, "Coffee", "Beverages", "12.50", 0);
        ProductView tea = product(2, "Tea", "Herbal", "3.25", 4);
        BrowseProducts browseProducts = new BrowseProducts(new FakeCatalogPort(List.of(coffee, tea)));

        List<ProductView> result = browseProducts.list();

        assertEquals(List.of(coffee, tea), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add(coffee));
    }

    @Test
    void searchesTrimmedCaseInsensitiveNameSubstringsAndReturnsAnImmutableEmptyResultWhenUnmatched() {
        ProductView coffee = product(1, "Coffee", "Beverages", "12.50", 0);
        ProductView tea = product(2, "Tea", "Herbal", "3.25", 4);
        BrowseProducts browseProducts = new BrowseProducts(new FakeCatalogPort(List.of(coffee, tea)));

        assertEquals(List.of(coffee), browseProducts.search(" OFF "));
        List<ProductView> unmatched = browseProducts.search("milk");
        assertEquals(List.of(), unmatched);
        assertThrows(UnsupportedOperationException.class, () -> unmatched.add(tea));
    }

    private ProductView product(long id, String name, String categoryName, String price, int stock) {
        return new ProductView(id, name, categoryName, new BigDecimal(price), stock);
    }

    private static final class FakeCatalogPort implements ProductCatalogPort {
        private final List<ProductView> products;

        private FakeCatalogPort(List<ProductView> products) {
            this.products = new ArrayList<>(products);
        }

        @Override
        public List<ProductView> listProducts() {
            return List.copyOf(products);
        }

        @Override
        public List<ProductView> searchProductsByName(String query) {
            String canonicalQuery = query.trim().toLowerCase(Locale.ROOT);
            return products.stream()
                    .filter(product -> product.name().toLowerCase(Locale.ROOT).contains(canonicalQuery))
                    .toList();
        }
    }
}
