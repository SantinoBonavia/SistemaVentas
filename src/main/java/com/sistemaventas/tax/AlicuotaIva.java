package com.sistemaventas.tax;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

/**
 * Alícuotas de IVA de la Ley de Impuesto al Valor Agregado (art. 28).
 * Las herramientas y artículos de ferretería tributan la alícuota general.
 */
public enum AlicuotaIva {
    GENERAL("21", "General"),
    REDUCIDA("10.5", "Reducida"),
    INCREMENTADA("27", "Incrementada"),
    EXENTO("0", "Exento");

    /** Fecha en la que se verificó que las alícuotas siguen vigentes. */
    public static final LocalDate VERIFICADO_AL = LocalDate.of(2026, 10, 24);

    private final BigDecimal percent;
    private final String description;

    AlicuotaIva(String percent, String description) {
        this.percent = new BigDecimal(percent);
        this.description = description;
    }

    public BigDecimal percent() {
        return percent;
    }

    public String description() {
        return description;
    }

    /** Formato argentino: "21%", "10,5%". */
    public String label() {
        return percent.stripTrailingZeros().toPlainString().replace('.', ',') + "%";
    }

    public static Optional<AlicuotaIva> fromPercent(BigDecimal percent) {
        if (percent == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(rate -> rate.percent.compareTo(percent) == 0).findFirst();
    }

    /** Acepta "21", "10,5", "10.5" o "21%". Vacío equivale a la alícuota general. */
    public static Optional<AlicuotaIva> parse(String input) {
        String value = input == null ? "" : input.trim().replace("%", "").trim().replace(',', '.');
        if (value.isEmpty()) {
            return Optional.of(GENERAL);
        }
        try {
            return fromPercent(new BigDecimal(value));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return label() + " – " + description;
    }
}
