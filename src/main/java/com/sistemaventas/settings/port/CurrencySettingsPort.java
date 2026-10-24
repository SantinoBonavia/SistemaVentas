package com.sistemaventas.settings.port;

import java.util.Currency;
import java.util.Optional;

public interface CurrencySettingsPort {
    Optional<Currency> load();

    void save(Currency currency);
}
