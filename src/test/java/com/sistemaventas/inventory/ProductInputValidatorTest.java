package com.sistemaventas.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.application.ProductInputValidator;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProductInputValidatorTest {

    @Test
    void acceptsPlainPositivePricesAndNormalizesThemToTwoDecimalPlacesWithoutRounding() {
        assertEquals(new BigDecimal("12.00"), ProductInputValidator.parsePositivePrice(" 12 ").orElseThrow());
        assertEquals(new BigDecimal("12.50"), ProductInputValidator.parsePositivePrice("12.5").orElseThrow());
        assertEquals(new BigDecimal("12.50"), ProductInputValidator.parsePositivePrice("12.50").orElseThrow());
    }

    @Test
    void acceptsArgentineCurrencyNotationAsExactScaleTwoAmounts() {
        assertEquals(new BigDecimal("2400.00"), ProductInputValidator.parsePositivePrice("2400,00").orElseThrow());
        assertEquals(new BigDecimal("2400.00"), ProductInputValidator.parsePositivePrice("2.400,00").orElseThrow());
        assertEquals(new BigDecimal("2400.00"), ProductInputValidator.parsePositivePrice("  $2.400,00  ").orElseThrow());
        assertEquals(new BigDecimal("12.50"), ProductInputValidator.parsePositivePrice("$12,5").orElseThrow());
    }

    @Test
    void rejectsNonPlainNonPositiveAndOverPrecisePrices() {
        for (String value : new String[]{
                "0", "$0,00", "-1", "$-1,00", "1E2", "12.345", "12,345", "12.", "12,",
                "2.40,00", "24.00,00", "2.4000,00", "2,400.00", "$ 2.400,00", "abc"
        }) {
            assertTrue(ProductInputValidator.parsePositivePrice(value).isEmpty(), value);
        }
    }

    @Test
    void trimsRequiredNamesAndAcceptsOnlyWholeNonNegativeStock() {
        assertEquals(Optional.of("Coffee"), ProductInputValidator.trimRequired(" Coffee "));
        assertTrue(ProductInputValidator.trimRequired(" ").isEmpty());
        assertEquals(Optional.of(0), ProductInputValidator.parseNonNegativeStock(" 0 "));
        assertEquals(Optional.of(12), ProductInputValidator.parseNonNegativeStock("12"));
        assertFalse(ProductInputValidator.parseNonNegativeStock("-1").isPresent());
        assertFalse(ProductInputValidator.parseNonNegativeStock("1.5").isPresent());
    }
}
