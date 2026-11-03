package com.sistemaventas.sales.application;

import java.io.Serializable;
import java.math.BigDecimal;

/** Renglón del comprobante con el precio final (IVA incluido) vigente al momento de la venta. */
public record SaleLine(long productId, String code, String name, BigDecimal ivaPercent, BigDecimal unitPrice,
                       int quantity) implements Serializable {

    public BigDecimal total() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
