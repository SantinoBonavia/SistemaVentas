package com.sistemaventas.sales.application;

import com.sistemaventas.sales.port.SalesPort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resumen de las ventas emitidas (no anuladas) en un rango de fechas. */
public final class SalesReport {
    private final SalesPort sales;

    public SalesReport(SalesPort sales) {
        this.sales = sales;
    }

    public record Summary(List<SaleRecord> sales, int count, BigDecimal total, BigDecimal iva, BigDecimal average,
                          Map<PaymentMethod, BigDecimal> byPayment, List<TopProduct> topProducts) {
    }

    public record TopProduct(String name, int quantity, BigDecimal total) {
    }

    /** @param from inclusive; null significa sin límite */
    public Summary between(LocalDate from, LocalDate to) {
        List<SaleRecord> inRange = sales.list().stream()
                .filter(sale -> from == null || !sale.dateTime().toLocalDate().isBefore(from))
                .filter(sale -> to == null || !sale.dateTime().toLocalDate().isAfter(to))
                .sorted(Comparator.comparing(SaleRecord::number).reversed())
                .toList();
        List<SaleRecord> valid = inRange.stream().filter(sale -> !sale.cancelled()).toList();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal iva = BigDecimal.ZERO;
        Map<PaymentMethod, BigDecimal> byPayment = new EnumMap<>(PaymentMethod.class);
        Map<String, TopProduct> products = new LinkedHashMap<>();
        for (SaleRecord sale : valid) {
            total = total.add(sale.totals().total());
            iva = iva.add(sale.totals().ivaTotal());
            byPayment.merge(sale.payment(), sale.totals().total(), BigDecimal::add);
            for (SaleLine line : sale.lines()) {
                products.merge(line.name(), new TopProduct(line.name(), line.quantity(), line.total()),
                        (a, b) -> new TopProduct(a.name(), a.quantity() + b.quantity(), a.total().add(b.total())));
            }
        }
        BigDecimal average = valid.isEmpty() ? BigDecimal.ZERO.setScale(2)
                : total.divide(BigDecimal.valueOf(valid.size()), 2, RoundingMode.HALF_UP);
        List<TopProduct> top = products.values().stream()
                .sorted(Comparator.comparing(TopProduct::quantity).reversed().thenComparing(TopProduct::name))
                .limit(5)
                .toList();
        return new Summary(inRange, valid.size(), total, iva, average, byPayment, top);
    }
}
