package com.sistemaventas.inventory.application;

import com.sistemaventas.inventory.port.ProductCatalogPort;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductRegistrationException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class UpdateProduct {
    private final ProductCatalogPort port;
    private final Map<String, PendingUpdate> pendingUpdates = new ConcurrentHashMap<>();

    public UpdateProduct(ProductCatalogPort port) {
        this.port = port;
    }

    public UpdateResult update(UpdateProductCommand command) {
        Validation validation = validate(command);
        if (validation.failure() != null) return validation.failure();
        PendingUpdate pending = validation.pending();
        if (port.findProductById(pending.id()).isEmpty()) return new UpdateResult.Failure("Product was not found.");
        if (port.productNameExistsExcludingId(pending.product().name(), pending.id())) {
            String token = UUID.randomUUID().toString();
            pendingUpdates.put(token, pending);
            return new UpdateResult.ConfirmationRequired(new ProductUpdateConfirmation(token));
        }
        return replace(pending);
    }

    public UpdateResult confirm(String token) {
        PendingUpdate pending = pendingUpdates.remove(token);
        return pending == null ? new UpdateResult.Failure("Update confirmation is invalid or has expired.") : replace(pending);
    }

    public void cancel(ProductUpdateConfirmation confirmation) {
        if (confirmation != null) pendingUpdates.remove(confirmation.token());
    }

    private UpdateResult replace(PendingUpdate pending) {
        try {
            port.updateProduct(pending.id(), pending.product());
            return new UpdateResult.Updated(pending.id());
        } catch (ProductRegistrationException exception) {
            return new UpdateResult.Failure("Product could not be updated.");
        }
    }

    private Validation validate(UpdateProductCommand command) {
        if (command == null || command.id() <= 0) return Validation.failed("id", "Product ID is required.");
        String name = ProductInputValidator.trimRequired(command.name()).orElse(null);
        if (name == null) return Validation.failed("name", "Product name is required.");
        String category = ProductInputValidator.trimRequired(command.categoryName()).orElse(null);
        if (category == null) return Validation.failed("category", "Category name is required.");
        var price = ProductInputValidator.parsePositivePrice(command.price());
        if (price.isEmpty()) return Validation.failed("price", "Price must be a positive amount with at most two decimal places.");
        var stock = ProductInputValidator.parseNonNegativeStock(command.stock());
        if (stock.isEmpty()) return Validation.failed("stock", "Stock must be a non-negative whole number.");
        return Validation.valid(new PendingUpdate(command.id(), new ProductDetails(name, price.orElseThrow(), category, stock.orElseThrow())));
    }

    private record PendingUpdate(long id, ProductDetails product) {}
    private record Validation(PendingUpdate pending, UpdateResult.ValidationFailed failure) {
        static Validation valid(PendingUpdate pending) { return new Validation(pending, null); }
        static Validation failed(String field, String message) { return new Validation(null, new UpdateResult.ValidationFailed(field, message)); }
    }
}
