package com.sistemaventas.inventory.port;

import java.util.Map;

/** Movimientos de stock atómicos: se aplican todos o ninguno. */
public interface StockPort {

    /** @param deltas cantidad a sumar (positiva) o restar (negativa) por id de producto */
    void applyStockChanges(Map<Long, Integer> deltas) throws StockException;

    final class StockException extends Exception {
        public StockException(String message) {
            super(message);
        }
    }
}
