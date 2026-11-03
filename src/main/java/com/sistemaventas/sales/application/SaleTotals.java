package com.sistemaventas.sales.application;

import com.sistemaventas.tax.IvaCalculator;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Totales de una venta. El descuento se aplica sobre los importes con IVA de cada alícuota
 * y luego se discrimina el neto gravado y el IVA de cada una.
 */
public record SaleTotals(BigDecimal subtotal, BigDecimal discountPercent, BigDecimal discount, BigDecimal total,
                         List<IvaBreakdown> breakdown) implements Serializable {

    public record IvaBreakdown(BigDecimal ivaPercent, BigDecimal net, BigDecimal iva) implements Serializable {
    }

    public static SaleTotals of(List<SaleLine> lines, BigDecimal discountPercent) {
        BigDecimal percent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
        BigDecimal keep = BigDecimal.ONE.subtract(percent.divide(new BigDecimal("100"), MathContext.DECIMAL64));
        Map<BigDecimal, BigDecimal> grossByRate = new TreeMap<>((a, b) -> b.compareTo(a));
        BigDecimal subtotal = BigDecimal.ZERO;
        for (SaleLine line : lines) {
            subtotal = subtotal.add(line.total());
            grossByRate.merge(line.ivaPercent().stripTrailingZeros(), line.total(), BigDecimal::add);
        }
        BigDecimal total = BigDecimal.ZERO;
        List<IvaBreakdown> breakdown = new java.util.ArrayList<>();
        for (Map.Entry<BigDecimal, BigDecimal> entry : grossByRate.entrySet()) {
            BigDecimal gross = IvaCalculator.money(entry.getValue().multiply(keep));
            BigDecimal net = IvaCalculator.netFromFinal(gross, entry.getKey());
            breakdown.add(new IvaBreakdown(entry.getKey(), net, gross.subtract(net)));
            total = total.add(gross);
        }
        subtotal = IvaCalculator.money(subtotal);
        return new SaleTotals(subtotal, percent, subtotal.subtract(total), total, List.copyOf(breakdown));
    }

    public BigDecimal netTotal() {
        return breakdown.stream().map(IvaBreakdown::net).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal ivaTotal() {
        return breakdown.stream().map(IvaBreakdown::iva).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
