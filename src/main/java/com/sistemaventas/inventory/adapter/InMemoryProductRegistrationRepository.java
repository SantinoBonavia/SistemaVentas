package com.sistemaventas.inventory.adapter;

import com.sistemaventas.inventory.port.ProductRegistrationPort;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductRegistrationException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InMemoryProductRegistrationRepository implements ProductRegistrationPort {
    private final Map<String, CategorySnapshot> categoriesByCanonicalName = new HashMap<>();
    private final Map<String, ProductSnapshot> productsByCanonicalName = new HashMap<>();
    private final List<CategorySnapshot> categories = new ArrayList<>();
    private final List<ProductSnapshot> products = new ArrayList<>();
    private long nextCategoryId = 1;
    private long nextProductId = 1;

    @Override
    public synchronized boolean productNameExists(String productName) {
        return productsByCanonicalName.containsKey(canonicalName(productName));
    }

    @Override
    public synchronized long createProductUsingExistingOrNewCategory(ProductDetails product)
            throws ProductRegistrationException {
        if (product == null) {
            throw new ProductRegistrationException("Product details are required.");
        }

        String productName = displayName(product.name());
        String categoryName = displayName(product.categoryName());
        CategorySnapshot category = categoriesByCanonicalName.get(canonicalName(categoryName));
        if (category == null) {
            category = new CategorySnapshot(nextCategoryId++, categoryName);
            categoriesByCanonicalName.put(canonicalName(categoryName), category);
            categories.add(category);
        }

        ProductSnapshot created = new ProductSnapshot(
                nextProductId++, productName, product.price(), category.id(), product.stock()
        );
        productsByCanonicalName.put(canonicalName(productName), created);
        products.add(created);
        return created.id();
    }

    synchronized List<CategorySnapshot> categories() {
        return List.copyOf(categories);
    }

    synchronized List<ProductSnapshot> products() {
        return List.copyOf(products);
    }

    private String canonicalName(String value) {
        return displayName(value).toLowerCase(Locale.ROOT);
    }

    private String displayName(String value) {
        return value == null ? "" : value.trim();
    }

    record CategorySnapshot(long id, String name) {
    }

    record ProductSnapshot(long id, String name, BigDecimal price, long categoryId, int stock) {
    }
}
