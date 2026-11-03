package com.sistemaventas.store;

import com.sistemaventas.tax.CondicionIva;
import com.sistemaventas.tax.Cuit;
import java.util.Objects;

/** Guarda los datos del comercio validando CUIT y punto de venta. */
public final class StoreSettings {
    private StoreProfile profile;
    private Runnable changeListener = () -> { };

    public StoreSettings() {
        this(StoreProfile.DEFAULT);
    }

    public StoreSettings(StoreProfile profile) {
        this.profile = Objects.requireNonNull(profile);
    }

    public synchronized StoreProfile profile() {
        return profile;
    }

    public synchronized void onChange(Runnable listener) {
        changeListener = Objects.requireNonNull(listener);
    }

    public synchronized void restore(StoreProfile saved) {
        profile = Objects.requireNonNull(saved);
    }

    public sealed interface Result {
        record Saved(StoreProfile profile) implements Result {
        }

        record Invalid(String message) implements Result {
        }
    }

    public synchronized Result save(String name, String legalName, String cuit, String address, String phone,
                                    CondicionIva condicion, String pointOfSale) {
        if (name == null || name.isBlank()) {
            return new Result.Invalid("El nombre de fantasía es obligatorio.");
        }
        String normalizedCuit = "";
        if (cuit != null && !cuit.isBlank()) {
            var parsed = Cuit.normalize(cuit);
            if (parsed.isEmpty()) {
                return new Result.Invalid("El CUIT no es válido. Revisá los 11 dígitos.");
            }
            normalizedCuit = parsed.orElseThrow();
        }
        int point;
        try {
            point = Integer.parseInt(pointOfSale == null ? "" : pointOfSale.trim());
        } catch (NumberFormatException exception) {
            point = -1;
        }
        if (point < 1 || point > 99998) {
            return new Result.Invalid("El punto de venta debe ser un número entre 1 y 99998.");
        }
        if (condicion == null || condicion == CondicionIva.CONSUMIDOR_FINAL) {
            return new Result.Invalid("Elegí la condición frente al IVA del comercio.");
        }
        profile = new StoreProfile(name, legalName, normalizedCuit, address, phone, condicion, point);
        changeListener.run();
        return new Result.Saved(profile);
    }
}
