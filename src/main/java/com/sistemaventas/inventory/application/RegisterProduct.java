package com.sistemaventas.inventory.application;

import com.sistemaventas.inventory.port.ProductRegistrationPort;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductRegistrationException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegisterProduct {
    private static final String SUCCESS_MESSAGE = "Producto cargado correctamente.";
    private static final String REGISTRATION_FAILURE_MESSAGE = "No se pudo cargar el producto. Intentá de nuevo.";
    private static final String CONFIRMATION_FAILURE_MESSAGE = "La confirmación de duplicado no es válida o expiró.";

    private final ProductRegistrationPort port;
    private final Map<String, ProductDetails> pendingDuplicates = new ConcurrentHashMap<>();

    public RegisterProduct(ProductRegistrationPort port) {
        this.port = port;
    }

    public RegistrationResult register(ProductRegistrationCommand command) {
        Validation validation = validate(command);
        if (validation.failure() != null) {
            return validation.failure();
        }

        ProductDetails product = validation.product();
        try {
            if (port.productNameExists(product.name())) {
                String token = UUID.randomUUID().toString();
                pendingDuplicates.put(token, product);
                return new RegistrationResult.ConfirmationRequired(new DuplicateConfirmation(token));
            }
            return create(product);
        } catch (ProductRegistrationException exception) {
            return registrationFailure();
        }
    }

    public RegistrationResult confirm(String token) {
        ProductDetails product = pendingDuplicates.remove(token);
        if (product == null) {
            return new RegistrationResult.Failure(CONFIRMATION_FAILURE_MESSAGE);
        }
        return create(product);
    }

    public void cancel(DuplicateConfirmation confirmation) {
        pendingDuplicates.remove(confirmation.token());
    }

    private RegistrationResult create(ProductDetails product) {
        try {
            return new RegistrationResult.Created(
                    port.createProductUsingExistingOrNewCategory(product),
                    SUCCESS_MESSAGE
            );
        } catch (ProductRegistrationException exception) {
            return registrationFailure();
        }
    }

    private Validation validate(ProductRegistrationCommand command) {
        if (command == null) {
            return Validation.failed("command", "Faltan los datos del producto.");
        }
        var parsed = ProductInputValidator.parseDetails(command.name(), command.categoryName(), command.price(),
                command.stock(), command.code(), command.ivaPercent(), command.minStock(), command.image());
        if (parsed.failed()) {
            return Validation.failed(parsed.field(), parsed.message());
        }
        String code = parsed.details().code();
        if (!code.isEmpty() && port.productCodeExists(code, 0)) {
            return Validation.failed("code", ProductInputValidator.DUPLICATE_CODE_MESSAGE);
        }
        return Validation.valid(parsed.details());
    }

    private RegistrationResult.Failure registrationFailure() {
        return new RegistrationResult.Failure(REGISTRATION_FAILURE_MESSAGE);
    }

    private record Validation(ProductDetails product, RegistrationResult.ValidationFailed failure) {
        private static Validation valid(ProductDetails product) {
            return new Validation(product, null);
        }

        private static Validation failed(String field, String message) {
            return new Validation(null, new RegistrationResult.ValidationFailed(field, message));
        }
    }
}
