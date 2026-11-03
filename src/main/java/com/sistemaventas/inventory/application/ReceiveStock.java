package com.sistemaventas.inventory.application;

import com.sistemaventas.inventory.port.ProductCatalogPort;
import com.sistemaventas.inventory.port.StockPort;
import com.sistemaventas.inventory.port.StockPort.StockException;
import java.util.Map;

/** Ingreso de mercadería o ajuste de stock (roturas, pérdidas, conteo). */
public final class ReceiveStock {
    private final ProductCatalogPort catalog;
    private final StockPort stock;

    public ReceiveStock(ProductCatalogPort catalog, StockPort stock) {
        this.catalog = catalog;
        this.stock = stock;
    }

    public sealed interface Result {
        record Done(ProductView product) implements Result {
        }

        record Rejected(String message) implements Result {
        }
    }

    public Result adjust(long productId, int delta) {
        if (delta == 0) {
            return new Result.Rejected("Ingresá una cantidad distinta de cero.");
        }
        if (catalog.findProductById(productId).isEmpty()) {
            return new Result.Rejected("No se encontró el producto.");
        }
        try {
            stock.applyStockChanges(Map.of(productId, delta));
        } catch (StockException exception) {
            return new Result.Rejected(exception.getMessage());
        }
        return new Result.Done(catalog.findProductById(productId).orElseThrow());
    }
}
