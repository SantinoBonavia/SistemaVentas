package com.sistemaventas.sales.application;

import com.sistemaventas.store.StoreProfile;
import com.sistemaventas.tax.AlicuotaIva;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Texto del ticket en 42 columnas, como en una impresora térmica de 80 mm.
 * Para comprobantes A discrimina neto e IVA por alícuota; para B y C informa el IVA contenido
 * (Régimen de Transparencia Fiscal al Consumidor, Ley 27.743).
 */
public final class TicketFormatter {
    public static final int WIDTH = 42;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private TicketFormatter() {
    }

    public static String format(SaleRecord sale, StoreProfile store) {
        List<String> out = new ArrayList<>();
        centered(out, store.name().toUpperCase(java.util.Locale.ROOT));
        if (!store.legalName().isEmpty()) centered(out, store.legalName());
        if (!store.address().isEmpty()) centered(out, store.address());
        if (!store.phone().isEmpty()) centered(out, "Tel. " + store.phone());
        centered(out, "CUIT " + (store.cuit().isEmpty() ? "(sin configurar)" : store.cuit()));
        centered(out, store.condicion().label());
        out.add(rule('='));
        out.add(columns("COMPROBANTE \"" + sale.letter() + "\"", "Nº " + sale.formattedNumber()));
        out.add("Fecha: " + DATE.format(sale.dateTime()));
        out.add("Cliente: " + sale.buyer().displayName());
        if (!sale.buyer().cuit().isEmpty()) {
            out.add("CUIT: " + sale.buyer().cuit());
            out.add(sale.buyer().condicion().label());
        }
        if (sale.cancelled()) {
            out.add(center("*** VENTA ANULADA ***"));
        }
        out.add(rule('-'));
        out.add(columns("Cant. x Precio unit.", "Importe"));
        out.add(rule('-'));
        boolean discriminate = "A".equals(sale.letter());
        for (SaleLine line : sale.lines()) {
            String rate = AlicuotaIva.fromPercent(line.ivaPercent()).map(AlicuotaIva::label)
                    .orElse(line.ivaPercent().toPlainString() + "%");
            out.addAll(wrap(line.name(), WIDTH));
            String detail = "  " + line.quantity() + " x " + Pesos.format(line.unitPrice()) + "  (IVA " + rate + ")";
            out.add(columns(detail, Pesos.format(line.total())));
            if (!line.code().isEmpty()) out.add("  Cód. " + line.code());
        }
        out.add(rule('-'));
        SaleTotals totals = sale.totals();
        if (totals.discount().signum() != 0) {
            out.add(columns("Subtotal", Pesos.format(totals.subtotal())));
            out.add(columns("Descuento " + percent(totals.discountPercent()), "-" + Pesos.format(totals.discount())));
        }
        if (discriminate) {
            for (SaleTotals.IvaBreakdown rate : totals.breakdown()) {
                String label = AlicuotaIva.fromPercent(rate.ivaPercent()).map(AlicuotaIva::label).orElse("");
                out.add(columns("Neto gravado " + label, Pesos.format(rate.net())));
                out.add(columns("IVA " + label, Pesos.format(rate.iva())));
            }
        }
        out.add(columns("TOTAL", Pesos.format(totals.total())));
        if (!discriminate) {
            out.add(rule('-'));
            out.add("Régimen de Transparencia Fiscal al");
            out.add("Consumidor (Ley 27.743)");
            out.add(columns("IVA contenido", Pesos.format(totals.ivaTotal())));
            out.add(columns("Otros imp. nac. indirectos", Pesos.format(BigDecimal.ZERO)));
        }
        out.add(rule('-'));
        out.add(columns("Forma de pago: " + sale.payment().label(), Pesos.format(totals.total())));
        if (sale.paidWith() != null) {
            out.add(columns("Recibido", Pesos.format(sale.paidWith())));
            out.add(columns("Vuelto", Pesos.format(sale.change())));
        }
        out.add(columns("Artículos", Integer.toString(sale.itemCount())));
        out.add(rule('='));
        out.add(center("¡Gracias por su compra!"));
        out.add(center("Comprobante interno"));
        out.add(center("No válido como factura"));
        return String.join("\n", out);
    }

    private static String percent(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString().replace('.', ',') + "%";
    }

    private static String rule(char c) {
        return String.valueOf(c).repeat(WIDTH);
    }

    private static void centered(List<String> out, String text) {
        wrap(text, WIDTH).forEach(line -> out.add(center(line)));
    }

    private static String center(String text) {
        if (text.length() >= WIDTH) return text.substring(0, WIDTH);
        return " ".repeat((WIDTH - text.length()) / 2) + text;
    }

    private static String columns(String left, String right) {
        int space = WIDTH - left.length() - right.length();
        if (space < 1) {
            return left + "\n" + " ".repeat(Math.max(0, WIDTH - right.length())) + right;
        }
        return left + " ".repeat(space) + right;
    }

    private static List<String> wrap(String text, int width) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : text.split("\\s+")) {
            while (word.length() > width) {
                if (current.length() > 0) {
                    lines.add(current.toString());
                    current.setLength(0);
                }
                lines.add(word.substring(0, width));
                word = word.substring(width);
            }
            if (current.length() > 0 && current.length() + 1 + word.length() > width) {
                lines.add(current.toString());
                current.setLength(0);
            }
            if (current.length() > 0) current.append(' ');
            current.append(word);
        }
        if (current.length() > 0) lines.add(current.toString());
        return lines;
    }
}
