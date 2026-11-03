package com.sistemaventas.inventory.application;

import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.tax.IvaCalculator;
import java.io.Serializable;
import java.math.BigDecimal;

/** Producto tal como lo ve la aplicación. {@code price} es el precio final con IVA incluido. */
public record ProductView(long id, String name, String categoryName, BigDecimal price, int stock, String code,
                          BigDecimal ivaPercent, int minStock, String image) implements Serializable {

    public enum StockStatus { DISPONIBLE, BAJO, SIN_STOCK }

    public ProductView {
        code = code == null ? "" : code;
        ivaPercent = ivaPercent == null ? ProductDetails.DEFAULT_IVA : ivaPercent;
        image = image == null ? "" : image;
    }

    public ProductView(long id, String name, String categoryName, BigDecimal price, int stock) {
        this(id, name, categoryName, price, stock, "", ProductDetails.DEFAULT_IVA, 0, "");
    }

    public BigDecimal netPrice() {
        return IvaCalculator.netFromFinal(price, ivaPercent);
    }

    public BigDecimal ivaAmount() {
        return IvaCalculator.ivaContained(price, ivaPercent);
    }

    public StockStatus stockStatus() {
        if (stock <= 0) {
            return StockStatus.SIN_STOCK;
        }
        return stock <= minStock ? StockStatus.BAJO : StockStatus.DISPONIBLE;
    }
}
