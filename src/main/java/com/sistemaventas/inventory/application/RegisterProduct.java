package com.sistemaventas.inventory.application;

import com.sistemaventas.inventory.port.ProductRegistrationPort;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductRegistrationException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class RegisterProduct {
    private static final String PRICE_MESSAGE = "Price must be a positive plain decimal with at most two fractional digits.";
    private static final String STOCK_MESSAGE = "Stock must be a non-negative whole number.";
    private static final String SUCCESS_MESSAGE = "Product registered successfully.";
    private static final String REGISTRATION_FAILURE_MESSAGE = "Product could not be registered. Please try again.";
    private static final String CONFIRMATION_FAILURE_MESSAGE = "Duplicate confirmation is invalid or has expired.";

    private final ProductRegistrationPort port;
    private final Map<String, ProductDetails> pendingDuplicates = new HashMap<>();

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
        String name = trim(command.name());
        if (name.isEmpty()) {
            return Validation.failed("name", "Product name is required.");
        }
        String categoryName = trim(command.categoryName());
        if (categoryName.isEmpty()) {
            return Validation.failed("category", "Category name is required.");
        }
        BigDecimal price = parsePrice(command.price());
        if (price == null) {
            return Validation.failed("price", PRICE_MESSAGE);
        }
        Integer stock = parseStock(command.stock());
        if (stock == null) {
            return Validation.failed("stock", STOCK_MESSAGE);
        }
        return Validation.valid(new ProductDetails(name, price, categoryName, stock));
    }

    private BigDecimal parsePrice(String input) {
        String value = trim(input);
        if (!value.matches("\\d+(?:\\.\\d{1,2})?")) {
            return null;
        }
        BigDecimal price = new BigDecimal(value);
        return price.signum() > 0 ? price : null;
    }

    private Integer parseStock(String input) {
        String value = trim(input);
        if (!value.matches("\\d+")) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
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
