package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PricePresenterTest {

    @Test
    void formatsArgentinePesosWithGroupingAndDecimalCommaWithoutMutatingTheAmount() {
        BigDecimal amount = new BigDecimal("2400.0");
        PricePresenter presenter = new PricePresenter(Optional.of(Currency.getInstance("ARS")));

        assertEquals("$2.400,00", presenter.format(amount));
        assertEquals(new BigDecimal("2400.0"), amount);
        assertEquals(Locale.forLanguageTag("es-AR"), presenter.locale());
    }

    @Test
    void presentsAClearDeterministicMessageWhenCurrencyIsUnconfigured() {
        PricePresenter presenter = new PricePresenter(Optional.empty(), Locale.ROOT);

        assertEquals("Currency not configured — 8.00", presenter.format(new BigDecimal("8")));
    }
}
