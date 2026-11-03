package com.sistemaventas.inventory.port;

import java.io.Serializable;
import java.math.BigDecimal;

public interface ProductRegistrationPort {

    boolean productNameExists(String productName) throws ProductRegistrationException;

    long createProductUsingExistingOrNewCategory(ProductDetails product) throws ProductRegistrationException;

    default boolean productCodeExists(String code, long excludingId) {
        return false;
    }

    /**
     * @param price      precio final al público, IVA incluido
     * @param ivaPercent alícuota de IVA en porcentaje (21, 10.5, 27, 0)
     * @param image      clave de ilustración ("art:martillo") o ruta a una foto; vacío si no tiene
     */
    record ProductDetails(String name, BigDecimal price, String categoryName, int stock, String code,
                          BigDecimal ivaPercent, int minStock, String image) implements Serializable {
        public static final BigDecimal DEFAULT_IVA = new BigDecimal("21");

        public ProductDetails {
            code = code == null ? "" : code.trim();
            ivaPercent = ivaPercent == null ? DEFAULT_IVA : ivaPercent;
            image = image == null ? "" : image;
        }

        public ProductDetails(String name, BigDecimal price, String categoryName, int stock) {
            this(name, price, categoryName, stock, "", DEFAULT_IVA, 0, "");
        }
    }

    final class ProductRegistrationException extends Exception {
        public ProductRegistrationException(String message) {
            super(message);
        }
    }
}
