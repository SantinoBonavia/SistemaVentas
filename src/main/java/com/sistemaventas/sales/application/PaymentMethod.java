package com.sistemaventas.sales.application;

public enum PaymentMethod {
    EFECTIVO("Efectivo"),
    DEBITO("Débito"),
    CREDITO("Crédito"),
    TRANSFERENCIA("Transferencia"),
    QR("QR / Billetera");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
