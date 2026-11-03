package com.sistemaventas.sales.application;

import com.sistemaventas.tax.CondicionIva;
import java.io.Serializable;
import java.util.Objects;

/** Receptor del comprobante. Para consumidor final el nombre y el CUIT son opcionales. */
public record Buyer(CondicionIva condicion, String name, String cuit) implements Serializable {
    public static final Buyer CONSUMIDOR_FINAL = new Buyer(CondicionIva.CONSUMIDOR_FINAL, "", "");

    public Buyer {
        Objects.requireNonNull(condicion);
        name = name == null ? "" : name.trim();
        cuit = cuit == null ? "" : cuit.trim();
    }

    public String displayName() {
        return name.isEmpty() ? condicion.label() : name;
    }
}
