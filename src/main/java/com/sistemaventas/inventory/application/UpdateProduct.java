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
        if (port.findProductById(pending.id()).isEmpty()) return new UpdateResult.Failure("No se encontró el producto.");
        if (port.productNameExistsExcludingId(pending.product().name(), pending.id())) {
            String token = UUID.randomUUID().toString();
            pendingUpdates.put(token, pending);
            return new UpdateResult.ConfirmationRequired(new ProductUpdateConfirmation(token));
        }
        return replace(pending);
    }

    public UpdateResult confirm(String token) {
        PendingUpdate pending = pendingUpdates.remove(token);
        return pending == null ? new UpdateResult.Failure("La confirmación no es válida o expiró.") : replace(pending);
    }

    public void cancel(ProductUpdateConfirmation confirmation) {
        if (confirmation != null) pendingUpdates.remove(confirmation.token());
    }

    private UpdateResult replace(PendingUpdate pending) {
        try {
            port.updateProduct(pending.id(), pending.product());
            return new UpdateResult.Updated(pending.id());
        } catch (ProductRegistrationException exception) {
            return new UpdateResult.Failure("No se pudo actualizar el producto.");
        }
    }

    private Validation validate(UpdateProductCommand command) {
        if (command == null || command.id() <= 0) return Validation.failed("id", "Falta el identificador del producto.");
        var parsed = ProductInputValidator.parseDetails(command.name(), command.categoryName(), command.price(),
                command.stock(), command.code(), command.ivaPercent(), command.minStock(), command.image());
        if (parsed.failed()) return Validation.failed(parsed.field(), parsed.message());
        String code = parsed.details().code();
        if (!code.isEmpty() && port.productCodeExists(code, command.id())) {
            return Validation.failed("code", ProductInputValidator.DUPLICATE_CODE_MESSAGE);
        }
        return Validation.valid(new PendingUpdate(command.id(), parsed.details()));
    }

    private record PendingUpdate(long id, ProductDetails product) {}
    private record Validation(PendingUpdate pending, UpdateResult.ValidationFailed failure) {
        static Validation valid(PendingUpdate pending) { return new Validation(pending, null); }
        static Validation failed(String field, String message) { return new Validation(null, new UpdateResult.ValidationFailed(field, message)); }
    }
}
