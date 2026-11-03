package com.sistemaventas.store;

import com.sistemaventas.tax.CondicionIva;
import java.io.Serializable;
import java.util.Objects;

/** Datos del comercio que encabezan los comprobantes. */
public record StoreProfile(String name, String legalName, String cuit, String address, String phone,
                           CondicionIva condicion, int pointOfSale) implements Serializable {

    public static final StoreProfile DEFAULT = new StoreProfile("Ferretería El Tornillo", "", "",
            "Av. Siempre Viva 742, Rosario, Santa Fe", "", CondicionIva.RESPONSABLE_INSCRIPTO, 1);

    public StoreProfile {
        name = clean(name);
        legalName = clean(legalName);
        cuit = clean(cuit);
        address = clean(address);
        phone = clean(phone);
        Objects.requireNonNull(condicion);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
