package com.sistemaventas.inventory.application;

import com.sistemaventas.inventory.port.ProductCatalogPort;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductRegistrationException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DeleteProduct {
    private final ProductCatalogPort port;
    private final Map<String, Long> pendingDeletions = new ConcurrentHashMap<>();

    public DeleteProduct(ProductCatalogPort port) {
        this.port = port;
    }

    public DeleteResult delete(long id) {
        if (id <= 0 || port.findProductById(id).isEmpty()) {
            return new DeleteResult.Failure("Product was not found.");
        }
        String token = UUID.randomUUID().toString();
        pendingDeletions.put(token, id);
        return new DeleteResult.ConfirmationRequired(new ProductDeletionConfirmation(token));
    }

    public DeleteResult confirm(String token) {
        Long id = token == null ? null : pendingDeletions.remove(token);
        if (id == null) {
            return new DeleteResult.Failure("Deletion confirmation is invalid or has expired.");
        }
        try {
            port.deleteProduct(id);
            return new DeleteResult.Deleted(id);
        } catch (ProductRegistrationException exception) {
            return new DeleteResult.Failure("Product could not be deleted.");
        }
    }

    public void cancel(ProductDeletionConfirmation confirmation) {
        if (confirmation != null && confirmation.token() != null) {
            pendingDeletions.remove(confirmation.token());
        }
    }
}
