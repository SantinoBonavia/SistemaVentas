package com.sistemaventas.sales.application;

import com.sistemaventas.inventory.port.StockPort;
import com.sistemaventas.inventory.port.StockPort.StockException;
import com.sistemaventas.sales.port.SalesPort;
import java.util.LinkedHashMap;
import java.util.Map;

/** Anula una venta y devuelve la mercadería al stock. */
public final class CancelSale {
    private final StockPort stock;
    private final SalesPort sales;

    public CancelSale(StockPort stock, SalesPort sales) {
        this.stock = stock;
        this.sales = sales;
    }

    public synchronized RegisterSale.Result cancel(long number) {
        SaleRecord sale = sales.find(number).orElse(null);
        if (sale == null) {
            return new RegisterSale.Result.Rejected("No se encontró la venta.");
        }
        if (sale.cancelled()) {
            return new RegisterSale.Result.Rejected("La venta ya estaba anulada.");
        }
        Map<Long, Integer> deltas = new LinkedHashMap<>();
        sale.lines().forEach(line -> deltas.merge(line.productId(), line.quantity(), Integer::sum));
        try {
            stock.applyStockChanges(deltas);
        } catch (StockException exception) {
            return new RegisterSale.Result.Rejected("No se pudo reponer el stock: " + exception.getMessage());
        }
        SaleRecord cancelled = sale.asCancelled();
        sales.save(cancelled);
        return new RegisterSale.Result.Completed(cancelled);
    }
}
