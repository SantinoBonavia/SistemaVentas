package com.sistemaventas.sales.application;

import com.sistemaventas.inventory.application.ProductView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Carrito del mostrador. Nunca permite más unidades que el stock conocido del producto. */
public final class Cart {
    private final Map<Long, Entry> entries = new LinkedHashMap<>();
    private BigDecimal discountPercent = BigDecimal.ZERO;

    private record Entry(ProductView product, int quantity) {
    }

    /** Suma una unidad. Devuelve un mensaje si no hay stock suficiente. */
    public Optional<String> add(ProductView product) {
        int current = quantityOf(product.id());
        return setQuantity(product, current + 1);
    }

    public Optional<String> setQuantity(ProductView product, int quantity) {
        if (quantity <= 0) {
            entries.remove(product.id());
            return Optional.empty();
        }
        if (quantity > product.stock()) {
            return Optional.of(product.stock() <= 0
                    ? "\"" + product.name() + "\" no tiene stock."
                    : "Solo hay " + product.stock() + " unidades de \"" + product.name() + "\".");
        }
        entries.put(product.id(), new Entry(product, quantity));
        return Optional.empty();
    }

    public void remove(long productId) {
        entries.remove(productId);
    }

    /** Actualiza precios y stock con el catálogo vigente; quita lo que ya no existe. */
    public void refresh(List<ProductView> catalog) {
        Map<Long, ProductView> byId = new LinkedHashMap<>();
        catalog.forEach(product -> byId.put(product.id(), product));
        for (Long id : new ArrayList<>(entries.keySet())) {
            ProductView current = byId.get(id);
            if (current == null) {
                entries.remove(id);
            } else {
                int quantity = Math.min(entries.get(id).quantity(), current.stock());
                if (quantity <= 0) entries.remove(id); else entries.put(id, new Entry(current, quantity));
            }
        }
    }

    public void clear() {
        entries.clear();
        discountPercent = BigDecimal.ZERO;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int quantityOf(long productId) {
        Entry entry = entries.get(productId);
        return entry == null ? 0 : entry.quantity();
    }

    public Optional<ProductView> product(long productId) {
        return Optional.ofNullable(entries.get(productId)).map(Entry::product);
    }

    public void setDiscountPercent(BigDecimal percent) {
        if (percent == null || percent.signum() < 0 || percent.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("El descuento debe estar entre 0% y 100%.");
        }
        discountPercent = percent;
    }

    public BigDecimal discountPercent() {
        return discountPercent;
    }

    public List<SaleLine> lines() {
        return entries.values().stream()
                .map(entry -> new SaleLine(entry.product().id(), entry.product().code(), entry.product().name(),
                        entry.product().ivaPercent(), entry.product().price(), entry.quantity()))
                .toList();
    }

    public int itemCount() {
        return entries.values().stream().mapToInt(Entry::quantity).sum();
    }

    public SaleTotals totals() {
        return SaleTotals.of(lines(), discountPercent);
    }
}
