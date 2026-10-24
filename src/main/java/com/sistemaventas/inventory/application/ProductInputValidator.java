package com.sistemaventas.inventory.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public final class ProductInputValidator {
    private static final String LEGACY_PRICE_PATTERN = "\\d+(?:\\.\\d{1,2})?";
    private static final String ARGENTINE_PRICE_PATTERN = "\\d+(?:,\\d{1,2})?";
    private static final String GROUPED_ARGENTINE_PRICE_PATTERN = "[1-9]\\d{0,2}(?:\\.\\d{3})+,\\d{1,2}";
    private static final String WHOLE_NUMBER_PATTERN = "\\d+";

    private ProductInputValidator() {
    }

    public static Optional<String> trimRequired(String input) {
        String value = trim(input);
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    public static Optional<BigDecimal> parsePositivePrice(String input) {
        String value = trim(input);
        boolean hasCurrencySymbol = value.startsWith("$");
        if (hasCurrencySymbol) {
            value = value.substring(1);
        }
        boolean legacyPrice = !hasCurrencySymbol && value.matches(LEGACY_PRICE_PATTERN);
        boolean argentinePrice = value.matches(ARGENTINE_PRICE_PATTERN)
                || value.matches(GROUPED_ARGENTINE_PRICE_PATTERN);
        if (!legacyPrice && !argentinePrice) {
            return Optional.empty();
        }
        String normalized = legacyPrice ? value : value.replace(".", "").replace(',', '.');
        BigDecimal price = new BigDecimal(normalized);
        if (price.signum() <= 0) {
            return Optional.empty();
        }
        return Optional.of(price.setScale(2, RoundingMode.UNNECESSARY));
    }

    public static Optional<Integer> parseNonNegativeStock(String input) {
        String value = trim(input);
        if (!value.matches(WHOLE_NUMBER_PATTERN)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.valueOf(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
