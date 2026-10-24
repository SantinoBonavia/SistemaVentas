package com.sistemaventas.settings.application;

import com.sistemaventas.settings.port.CurrencySettingsPort;
import java.util.Currency;
import java.util.Locale;
import java.util.Optional;

public final class ManageCurrency {
    private static final String INVALID_CURRENCY_MESSAGE = "Currency must be a valid ISO-4217 code.";

    private final CurrencySettingsPort port;

    public ManageCurrency(CurrencySettingsPort port) {
        this.port = port;
    }

    public Optional<Currency> load() {
        return port.load();
    }

    public Result save(String input) {
        try {
            Currency currency = Currency.getInstance(normalize(input));
            port.save(currency);
            return new Result.Saved(currency);
        } catch (IllegalArgumentException exception) {
            return new Result.ValidationFailed(INVALID_CURRENCY_MESSAGE);
        }
    }

    private String normalize(String input) {
        return input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
    }

    public sealed interface Result {
        record Saved(Currency currency) implements Result {
        }

        record ValidationFailed(String message) implements Result {
        }
    }
}
