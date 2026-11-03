package com.sistemaventas.tax;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Cálculos de IVA sobre importes en pesos. Los precios al público se guardan con IVA incluido
 * (como se exhiben en mostrador) y el neto y el IVA contenido se derivan de ellos.
 */
public final class IvaCalculator {
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private IvaCalculator() {
    }

    public static BigDecimal finalFromNet(BigDecimal net, BigDecimal ratePercent) {
        return money(net.multiply(factor(ratePercent)));
    }

    public static BigDecimal netFromFinal(BigDecimal finalPrice, BigDecimal ratePercent) {
        return money(finalPrice.divide(factor(ratePercent), MathContext.DECIMAL64));
    }

    /** IVA contenido en un precio final: final − neto, de modo que ambos sumen exactamente el final. */
    public static BigDecimal ivaContained(BigDecimal finalPrice, BigDecimal ratePercent) {
        return money(finalPrice).subtract(netFromFinal(finalPrice, ratePercent));
    }

    public static BigDecimal money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal factor(BigDecimal ratePercent) {
        return BigDecimal.ONE.add(ratePercent.divide(HUNDRED, MathContext.DECIMAL64));
    }
}
