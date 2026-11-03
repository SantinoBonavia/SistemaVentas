package com.sistemaventas.inventory.application;

import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.tax.AlicuotaIva;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

public final class ProductInputValidator {
    private static final String LEGACY_PRICE_PATTERN = "\\d+(?:\\.\\d{1,2})?";
    private static final String ARGENTINE_PRICE_PATTERN = "\\d+(?:,\\d{1,2})?";
    private static final String GROUPED_ARGENTINE_PRICE_PATTERN = "[1-9]\\d{0,2}(?:\\.\\d{3})+(?:,\\d{1,2})?";
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

    /** Valida todos los campos del formulario de producto, en el orden en que se muestran. */
    static ParsedDetails parseDetails(String name, String categoryName, String price, String stock, String code,
                                      String ivaPercent, String minStock, String image) {
        String trimmedName = trimRequired(name).orElse(null);
        if (trimmedName == null) {
            return ParsedDetails.failed("name", NAME_MESSAGE);
        }
        String category = trimRequired(categoryName).orElse(null);
        if (category == null) {
            return ParsedDetails.failed("category", CATEGORY_MESSAGE);
        }
        Optional<BigDecimal> parsedPrice = parsePositivePrice(price);
        if (parsedPrice.isEmpty()) {
            return ParsedDetails.failed("price", PRICE_MESSAGE);
        }
        Optional<Integer> parsedStock = parseNonNegativeStock(stock);
        if (parsedStock.isEmpty()) {
            return ParsedDetails.failed("stock", STOCK_MESSAGE);
        }
        Optional<AlicuotaIva> rate = AlicuotaIva.parse(ivaPercent);
        if (rate.isEmpty()) {
            return ParsedDetails.failed("iva", IVA_MESSAGE);
        }
        Optional<Integer> parsedMinStock = trim(minStock).isEmpty() ? Optional.of(0) : parseNonNegativeStock(minStock);
        if (parsedMinStock.isEmpty()) {
            return ParsedDetails.failed("minStock", MIN_STOCK_MESSAGE);
        }
        String trimmedCode = trim(code).toUpperCase(java.util.Locale.ROOT);
        if (!trimmedCode.matches("[A-Z0-9-]{0,20}")) {
            return ParsedDetails.failed("code", CODE_MESSAGE);
        }
        return new ParsedDetails(new ProductDetails(trimmedName, parsedPrice.orElseThrow(), category,
                parsedStock.orElseThrow(), trimmedCode, rate.orElseThrow().percent(), parsedMinStock.orElseThrow(),
                image), null, null);
    }

    static final String NAME_MESSAGE = "El nombre del producto es obligatorio.";
    static final String CATEGORY_MESSAGE = "La categoría es obligatoria.";
    static final String PRICE_MESSAGE = "El precio debe ser un importe positivo con hasta dos decimales.";
    static final String STOCK_MESSAGE = "El stock debe ser un número entero mayor o igual a cero.";
    static final String IVA_MESSAGE = "La alícuota de IVA debe ser 21%, 10,5%, 27% o 0%.";
    static final String MIN_STOCK_MESSAGE = "El stock mínimo debe ser un número entero mayor o igual a cero.";
    static final String CODE_MESSAGE = "El código solo admite letras, números y guiones (hasta 20).";
    static final String DUPLICATE_CODE_MESSAGE = "Ya existe otro producto con ese código.";

    record ParsedDetails(ProductDetails details, String field, String message) {
        static ParsedDetails failed(String field, String message) {
            return new ParsedDetails(null, field, message);
        }

        boolean failed() {
            return details == null;
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
