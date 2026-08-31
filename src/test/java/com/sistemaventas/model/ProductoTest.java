package com.sistemaventas.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProductoTest {

    @Test
    void preservesAnExactTwoDecimalPriceFromItsSchemaAlignedConstructor() {
        BigDecimal price = new BigDecimal("12.50");
        Producto product = new Producto(1, "Coffee", price, 0, new Categoria());

        assertInstanceOf(BigDecimal.class, product.getPrecio());
        assertEquals(new BigDecimal("12.50"), product.getPrecio());
        assertEquals(2, product.getPrecio().scale());
    }

    @Test
    void preservesAnotherExactPriceWhenUpdated() {
        Producto product = new Producto();
        BigDecimal price = new BigDecimal("99.99");

        product.setPrecio(price);

        assertEquals(new BigDecimal("99.99"), product.getPrecio());
        assertEquals(2, product.getPrecio().scale());
    }

    @Test
    void canonicalSchemaEnforcesExactProductPriceAndCategoryUniqueness() throws IOException {
        String schema = Files.readString(Path.of("sql", "sistema_ventas.sql"));

        assertTrue(schema.contains("`precio` decimal(10,2) NOT NULL"));
        assertTrue(schema.contains("CONSTRAINT `chk_productos_precio` CHECK ((`precio` > 0))"));
        assertTrue(schema.contains("UNIQUE KEY `categorias_nombre_unique` (`nombre`)"));
        assertTrue(schema.contains("CREATE TABLE `app_settings`"));
    }
}
