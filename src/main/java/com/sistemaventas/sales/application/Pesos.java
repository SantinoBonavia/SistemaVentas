package com.sistemaventas.sales.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** Formato de pesos argentinos: $18.500,00. */
public final class Pesos {
    private static final Locale ARGENTINA = Locale.forLanguageTag("es-AR");

    private Pesos() {
    }

    public static String format(BigDecimal amount) {
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        String digits = numberFormat("#,##0.00").format(value.abs());
        return (value.signum() < 0 ? "-$" : "$") + digits;
    }

    /** Sin decimales cuando el importe es entero ($18.500), útil para tarjetas y carteles. */
    public static String compact(BigDecimal amount) {
        BigDecimal value = amount.setScale(2, RoundingMode.HALF_UP);
        if (value.stripTrailingZeros().scale() <= 0) {
            return (value.signum() < 0 ? "-$" : "$") + numberFormat("#,##0").format(value.abs());
        }
        return format(value);
    }

    /** Texto editable para un campo de importe: 18.500,00 */
    public static String plain(BigDecimal amount) {
        return numberFormat("#,##0.00").format(amount.setScale(2, RoundingMode.HALF_UP));
    }

    private static DecimalFormat numberFormat(String pattern) {
        DecimalFormat format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(ARGENTINA));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    }
}
