package com.sistemaventas.inventory.port;

import java.math.BigDecimal;

public interface ProductRegistrationPort {

    boolean productNameExists(String productName) throws ProductRegistrationException;

    long createProductUsingExistingOrNewCategory(ProductDetails product) throws ProductRegistrationException;

    record ProductDetails(String name, BigDecimal price, String categoryName, int stock) {
    }

    final class ProductRegistrationException extends Exception {
        public ProductRegistrationException(String message) {
            super(message);
        }
    }
}
