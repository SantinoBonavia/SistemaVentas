package com.sistemaventas.tax;

/** Condición frente al IVA del vendedor o del comprador. */
public enum CondicionIva {
    RESPONSABLE_INSCRIPTO("IVA Responsable Inscripto"),
    MONOTRIBUTISTA("Responsable Monotributo"),
    EXENTO("IVA Sujeto Exento"),
    CONSUMIDOR_FINAL("Consumidor Final");

    private final String label;

    CondicionIva(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /**
     * Letra del comprobante según las condiciones de emisor y receptor:
     * RI a RI o monotributista → A; RI a consumidor final o exento → B; monotributista → C.
     */
    public static String comprobanteLetter(CondicionIva seller, CondicionIva buyer) {
        if (seller != RESPONSABLE_INSCRIPTO) {
            return "C";
        }
        return buyer == RESPONSABLE_INSCRIPTO || buyer == MONOTRIBUTISTA ? "A" : "B";
    }

    @Override
    public String toString() {
        return label;
    }
}
