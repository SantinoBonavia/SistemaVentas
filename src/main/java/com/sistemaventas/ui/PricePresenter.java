package com.sistemaventas.ui;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/** Formats stored amounts without changing their scale or value. */
public final class PricePresenter {
    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");
    private final Optional<Currency> currency;
    private final Locale locale;

    public PricePresenter(Optional<Currency> currency) {
        this(currency, ARGENTINA);
    }

    public PricePresenter(Optional<Currency> currency, Locale locale) {
        this.currency = Objects.requireNonNull(currency);
        this.locale = Objects.requireNonNull(locale);
    }

    public String format(BigDecimal amount) {
        BigDecimal exactAmount = Objects.requireNonNull(amount).setScale(2, RoundingMode.UNNECESSARY);
        NumberFormat numbers = NumberFormat.getNumberInstance(locale);
        numbers.setGroupingUsed(true);
        numbers.setMinimumFractionDigits(2);
        numbers.setMaximumFractionDigits(2);
        String value = numbers.format(exactAmount);
        return currency.map(valueCurrency -> valueCurrency.getSymbol(locale) + value)
                .orElse("Currency not configured — " + value);
    }

    public Locale locale() {
        return locale;
    }
}
