package com.sistemaventas.inventory.port;

import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import java.util.List;
import java.util.Optional;

public interface ProductCatalogPort {

    List<ProductView> listProducts();

    List<ProductView> searchProductsByName(String query);

    default Optional<ProductView> findProductById(long id) {
        return Optional.empty();
    }

    default boolean productNameExistsExcludingId(String productName, long id) {
        return false;
    }

    default void updateProduct(long id, ProductDetails product) throws ProductRegistrationPort.ProductRegistrationException {
        throw new ProductRegistrationPort.ProductRegistrationException("Product updates are not supported.");
    }

    default void deleteProduct(long id) throws ProductRegistrationPort.ProductRegistrationException {
        throw new ProductRegistrationPort.ProductRegistrationException("Product deletion is not supported.");
    }
}
