package com.sistemaventas.sales.application;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** Venta emitida. {@code paidWith} y {@code change} solo aplican al pago en efectivo. */
public record SaleRecord(long number, int pointOfSale, String letter, LocalDateTime dateTime, List<SaleLine> lines,
                         SaleTotals totals, PaymentMethod payment, Buyer buyer, BigDecimal paidWith,
                         BigDecimal change, boolean cancelled) implements Serializable {

    public SaleRecord {
        lines = List.copyOf(lines);
    }

    /** Número con el formato de los comprobantes argentinos: 00001-00000042. */
    public String formattedNumber() {
        return String.format("%05d-%08d", pointOfSale, number);
    }

    public int itemCount() {
        return lines.stream().mapToInt(SaleLine::quantity).sum();
    }

    public SaleRecord asCancelled() {
        return new SaleRecord(number, pointOfSale, letter, dateTime, lines, totals, payment, buyer, paidWith, change, true);
    }
}
