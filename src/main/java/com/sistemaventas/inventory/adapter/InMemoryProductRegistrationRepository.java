package com.sistemaventas.inventory.adapter;

import com.sistemaventas.inventory.port.ProductRegistrationPort;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductRegistrationException;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.port.ProductCatalogPort;
import com.sistemaventas.inventory.port.StockPort;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryProductRegistrationRepository
        implements ProductRegistrationPort, ProductCatalogPort, StockPort {
    private final Map<String, CategorySnapshot> categoriesByCanonicalName = new HashMap<>();
    private final Map<Long, CategorySnapshot> categoriesById = new LinkedHashMap<>();
    private final Map<String, List<Long>> productIdsByCanonicalName = new HashMap<>();
    private final Map<Long, ProductSnapshot> productsById = new LinkedHashMap<>();
    private long nextCategoryId = 1;
    private long nextProductId = 1;
    private Runnable changeListener = () -> { };

    /** Se invoca después de cada modificación, por ejemplo para guardar los datos en disco. */
    public synchronized void onChange(Runnable listener) {
        changeListener = Objects.requireNonNull(listener);
    }

    @Override
    public synchronized boolean productNameExists(String productName) {
        return productIdsByCanonicalName.containsKey(canonicalName(productName));
    }

    @Override
    public synchronized boolean productCodeExists(String code, long excludingId) {
        String canonicalCode = canonicalName(code);
        return !canonicalCode.isEmpty() && productsById.values().stream()
                .anyMatch(product -> product.id() != excludingId && canonicalName(product.code()).equals(canonicalCode));
    }

    @Override
    public synchronized long createProductUsingExistingOrNewCategory(ProductDetails product)
            throws ProductRegistrationException {
        if (product == null) {
            throw new ProductRegistrationException("Faltan los datos del producto.");
        }

        String productName = displayName(product.name());
        CategorySnapshot category = categoryFor(product.categoryName());
        ProductSnapshot created = snapshot(nextProductId++, productName, category.id(), product);
        productIdsByCanonicalName.computeIfAbsent(canonicalName(productName), ignored -> new ArrayList<>()).add(created.id());
        productsById.put(created.id(), created);
        changed();
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
    public synchronized Optional<ProductView> findProductById(long id) {
        ProductSnapshot product = productsById.get(id);
        return product == null ? Optional.empty() : Optional.of(productView(product));
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
            throw new ProductRegistrationException("No se pudo actualizar el producto.");
        }
        String name = displayName(product.name());
        CategorySnapshot category = categoryFor(product.categoryName());
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
        productsById.put(id, snapshot(id, name, category.id(), product));
        changed();
    }

    @Override
    public synchronized void deleteProduct(long id) throws ProductRegistrationException {
        ProductSnapshot deleted = productsById.remove(id);
        if (deleted == null) {
            throw new ProductRegistrationException("No se pudo eliminar el producto.");
        }
        String canonicalName = canonicalName(deleted.name());
        List<Long> ids = productIdsByCanonicalName.get(canonicalName);
        ids.remove(id);
        if (ids.isEmpty()) {
            productIdsByCanonicalName.remove(canonicalName);
        }
        changed();
    }

    @Override
    public synchronized void applyStockChanges(Map<Long, Integer> deltas) throws StockException {
        for (Map.Entry<Long, Integer> change : deltas.entrySet()) {
            ProductSnapshot product = productsById.get(change.getKey());
            if (product == null) {
                throw new StockException("Un producto del ticket ya no existe en el inventario.");
            }
            if ((long) product.stock() + change.getValue() < 0) {
                throw new StockException("Stock insuficiente de \"" + product.name() + "\": quedan "
                        + product.stock() + " unidades.");
            }
        }
        deltas.forEach((id, delta) -> productsById.computeIfPresent(id, (ignored, product) -> product.withStock(product.stock() + delta)));
        changed();
    }

    public synchronized List<String> categoryNames() {
        return categoriesById.values().stream().map(CategorySnapshot::name).toList();
    }

    /** Estado completo, para guardarlo en disco. */
    public synchronized State exportState() {
        return new State(List.copyOf(categoriesById.values()), List.copyOf(productsById.values()), nextCategoryId, nextProductId);
    }

    public synchronized void importState(State state) {
        categoriesByCanonicalName.clear();
        categoriesById.clear();
        productIdsByCanonicalName.clear();
        productsById.clear();
        for (CategorySnapshot category : state.categories()) {
            categoriesById.put(category.id(), category);
            categoriesByCanonicalName.put(canonicalName(category.name()), category);
        }
        for (ProductSnapshot product : state.products()) {
            productsById.put(product.id(), product);
            productIdsByCanonicalName.computeIfAbsent(canonicalName(product.name()), ignored -> new ArrayList<>()).add(product.id());
        }
        nextCategoryId = state.nextCategoryId();
        nextProductId = state.nextProductId();
    }

    synchronized List<CategorySnapshot> categories() {
        return List.copyOf(categoriesById.values());
    }

    synchronized List<ProductSnapshot> products() {
        return List.copyOf(productsById.values());
    }

    private CategorySnapshot categoryFor(String rawName) {
        String categoryName = displayName(rawName);
        CategorySnapshot category = categoriesByCanonicalName.get(canonicalName(categoryName));
        if (category == null) {
            category = new CategorySnapshot(nextCategoryId++, categoryName);
            categoriesByCanonicalName.put(canonicalName(categoryName), category);
            categoriesById.put(category.id(), category);
        }
        return category;
    }

    private static ProductSnapshot snapshot(long id, String name, long categoryId, ProductDetails product) {
        return new ProductSnapshot(id, name, product.price(), categoryId, product.stock(), product.code(),
                product.ivaPercent(), product.minStock(), product.image());
    }

    private void changed() {
        changeListener.run();
    }

    private List<ProductView> productViews() {
        return productsById.values().stream()
                .map(this::productView)
                .toList();
    }

    private ProductView productView(ProductSnapshot product) {
        return new ProductView(product.id(), product.name(), categoriesById.get(product.categoryId()).name(),
                product.price(), product.stock(), product.code(), product.ivaPercent(), product.minStock(),
                product.image());
    }

    private String canonicalName(String value) {
        return displayName(value).toLowerCase(Locale.ROOT);
    }

    private String displayName(String value) {
        return value == null ? "" : value.trim();
    }

    public record State(List<CategorySnapshot> categories, List<ProductSnapshot> products, long nextCategoryId,
                        long nextProductId) implements Serializable {
    }

    public record CategorySnapshot(long id, String name) implements Serializable {
    }

    public record ProductSnapshot(long id, String name, BigDecimal price, long categoryId, int stock, String code,
                                  BigDecimal ivaPercent, int minStock, String image) implements Serializable {
        ProductSnapshot(long id, String name, BigDecimal price, long categoryId, int stock) {
            this(id, name, price, categoryId, stock, "", ProductDetails.DEFAULT_IVA, 0, "");
        }

        ProductSnapshot withStock(int newStock) {
            return new ProductSnapshot(id, name, price, categoryId, newStock, code, ivaPercent, minStock, image);
        }
    }
}
