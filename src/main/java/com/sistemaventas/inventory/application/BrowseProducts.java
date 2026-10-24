package com.sistemaventas.inventory.application;

import com.sistemaventas.inventory.port.ProductCatalogPort;
import java.util.List;
import java.util.Locale;

public final class BrowseProducts {
    private final ProductCatalogPort port;

    public BrowseProducts(ProductCatalogPort port) {
        this.port = port;
    }

    public List<ProductView> list() {
        return List.copyOf(port.listProducts());
    }

    public List<ProductView> search(String query) {
        String trimmedQuery = query == null ? "" : query.trim();
        if (trimmedQuery.isEmpty()) {
            return list();
        }
        String canonicalQuery = trimmedQuery.toLowerCase(Locale.ROOT);
        return list().stream()
                .filter(product -> product.name().toLowerCase(Locale.ROOT).contains(canonicalQuery))
                .toList();
    }
}
