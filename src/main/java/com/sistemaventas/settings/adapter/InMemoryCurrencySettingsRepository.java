package com.sistemaventas.settings.adapter;

import com.sistemaventas.settings.port.CurrencySettingsPort;
import java.util.Currency;
import java.util.Optional;

public final class InMemoryCurrencySettingsRepository implements CurrencySettingsPort {
    private Currency currency = Currency.getInstance("ARS");

    @Override
    public synchronized Optional<Currency> load() {
        return Optional.ofNullable(currency);
    }

    @Override
    public synchronized void save(Currency currency) {
        this.currency = currency;
    }
}
