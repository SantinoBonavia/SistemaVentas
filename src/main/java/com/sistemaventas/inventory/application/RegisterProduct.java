package com.sistemaventas.inventory.application;

import com.sistemaventas.inventory.port.ProductRegistrationPort;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductRegistrationException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class RegisterProduct {
    private static final String PRICE_MESSAGE = "Price must be a positive amount with at most two decimal places.";
    private static final String STOCK_MESSAGE = "Stock must be a non-negative whole number.";
    private static final String SUCCESS_MESSAGE = "Product registered successfully.";
    private static final String REGISTRATION_FAILURE_MESSAGE = "Product could not be registered. Please try again.";
    private static final String CONFIRMATION_FAILURE_MESSAGE = "Duplicate confirmation is invalid or has expired.";

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
            return Validation.failed("command", "Product registration details are required.");
        }
        String name = ProductInputValidator.trimRequired(command.name()).orElse(null);
        if (name == null) {
            return Validation.failed("name", "Product name is required.");
        }
        String categoryName = ProductInputValidator.trimRequired(command.categoryName()).orElse(null);
        if (categoryName == null) {
            return Validation.failed("category", "Category name is required.");
        }
        var price = ProductInputValidator.parsePositivePrice(command.price());
        if (price.isEmpty()) {
            return Validation.failed("price", PRICE_MESSAGE);
        }
        var stock = ProductInputValidator.parseNonNegativeStock(command.stock());
        if (stock.isEmpty()) {
            return Validation.failed("stock", STOCK_MESSAGE);
        }
        return Validation.valid(new ProductDetails(name, price.orElseThrow(), categoryName, stock.orElseThrow()));
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
