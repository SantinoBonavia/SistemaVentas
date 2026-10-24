package com.sistemaventas.inventory.adapter;

import com.sistemaventas.inventory.port.ProductRegistrationPort;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductRegistrationException;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.port.ProductCatalogPort;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InMemoryProductRegistrationRepository implements ProductRegistrationPort, ProductCatalogPort {
    private final Map<String, CategorySnapshot> categoriesByCanonicalName = new HashMap<>();
    private final Map<Long, CategorySnapshot> categoriesById = new LinkedHashMap<>();
    private final Map<String, List<Long>> productIdsByCanonicalName = new HashMap<>();
    private final Map<Long, ProductSnapshot> productsById = new LinkedHashMap<>();
    private long nextCategoryId = 1;
    private long nextProductId = 1;

    @Override
    public synchronized boolean productNameExists(String productName) {
        return productIdsByCanonicalName.containsKey(canonicalName(productName));
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
            categoriesById.put(category.id(), category);
        }

        ProductSnapshot created = new ProductSnapshot(
                nextProductId++, productName, product.price(), category.id(), product.stock()
        );
        productIdsByCanonicalName.computeIfAbsent(canonicalName(productName), ignored -> new ArrayList<>()).add(created.id());
        productsById.put(created.id(), created);
        return created.id();
    }

    @Override
    public synchronized List<ProductView> listProducts() {
        return List.copyOf(productViews());
    }

    @Override
    public synchronized List<ProductView> searchProductsByName(String query) {
        String canonicalQuery = canonicalName(query);
        return productViews().stream()
                .filter(product -> product.name().toLowerCase(Locale.ROOT).contains(canonicalQuery))
                .toList();
    }

    @Override
    public synchronized java.util.Optional<ProductView> findProductById(long id) {
        ProductSnapshot product = productsById.get(id);
        return product == null ? java.util.Optional.empty() : java.util.Optional.of(productView(product));
    }

    @Override
    public synchronized boolean productNameExistsExcludingId(String productName, long id) {
        return productIdsByCanonicalName.getOrDefault(canonicalName(productName), List.of()).stream()
                .anyMatch(productId -> productId != id);
    }

    @Override
    public synchronized void updateProduct(long id, ProductDetails product) throws ProductRegistrationException {
        ProductSnapshot existing = productsById.get(id);
        if (existing == null || product == null) {
            throw new ProductRegistrationException("Product could not be updated.");
        }
        String name = displayName(product.name());
        String categoryName = displayName(product.categoryName());
        CategorySnapshot category = categoriesByCanonicalName.get(canonicalName(categoryName));
        if (category == null) {
            category = new CategorySnapshot(nextCategoryId++, categoryName);
            categoriesByCanonicalName.put(canonicalName(categoryName), category);
            categoriesById.put(category.id(), category);
        }
        String oldCanonicalName = canonicalName(existing.name());
        String newCanonicalName = canonicalName(name);
        if (!oldCanonicalName.equals(newCanonicalName)) {
            List<Long> ids = productIdsByCanonicalName.get(oldCanonicalName);
            ids.remove(id);
            if (ids.isEmpty()) {
                productIdsByCanonicalName.remove(oldCanonicalName);
            }
            productIdsByCanonicalName.computeIfAbsent(newCanonicalName, ignored -> new ArrayList<>()).add(id);
        }
        productsById.put(id, new ProductSnapshot(id, name, product.price(), category.id(), product.stock()));
    }

    @Override
    public synchronized void deleteProduct(long id) throws ProductRegistrationException {
        ProductSnapshot deleted = productsById.remove(id);
        if (deleted == null) {
            throw new ProductRegistrationException("Product could not be deleted.");
        }
        String canonicalName = canonicalName(deleted.name());
        List<Long> ids = productIdsByCanonicalName.get(canonicalName);
        ids.remove(id);
        if (ids.isEmpty()) {
            productIdsByCanonicalName.remove(canonicalName);
        }
    }

    synchronized List<CategorySnapshot> categories() {
        return List.copyOf(categoriesById.values());
    }

    synchronized List<ProductSnapshot> products() {
        return List.copyOf(productsById.values());
    }

    private List<ProductView> productViews() {
        return productsById.values().stream()
                .map(this::productView)
                .toList();
    }

    private ProductView productView(ProductSnapshot product) {
        return new ProductView(product.id(), product.name(), categoriesById.get(product.categoryId()).name(),
                product.price(), product.stock());
    }

    private String canonicalName(String value) {
        return displayName(value).toLowerCase(Locale.ROOT);
    }

    private String displayName(String value) {
        return value == null ? "" : value.trim();
    }

    record CategorySnapshot(long id, String name) {
    }

    record ProductSnapshot(long id, String name, java.math.BigDecimal price, long categoryId, int stock) {
    }
}
