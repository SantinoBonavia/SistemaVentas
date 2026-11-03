package com.sistemaventas.sales.adapter;

import com.sistemaventas.sales.application.SaleRecord;
import com.sistemaventas.sales.port.SalesPort;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemorySalesRepository implements SalesPort {
    private final Map<Long, SaleRecord> sales = new LinkedHashMap<>();
    private long lastNumber;
    private Runnable changeListener = () -> { };

    public synchronized void onChange(Runnable listener) {
        changeListener = Objects.requireNonNull(listener);
    }

    @Override
    public synchronized long nextNumber() {
        return ++lastNumber;
    }

    @Override
    public synchronized void save(SaleRecord sale) {
        sales.put(sale.number(), sale);
        lastNumber = Math.max(lastNumber, sale.number());
        changeListener.run();
    }

    @Override
    public synchronized List<SaleRecord> list() {
        return List.copyOf(sales.values());
    }

    @Override
    public synchronized Optional<SaleRecord> find(long number) {
        return Optional.ofNullable(sales.get(number));
    }

    public synchronized State exportState() {
        return new State(new ArrayList<>(sales.values()), lastNumber);
    }

    public synchronized void importState(State state) {
        sales.clear();
        state.sales().forEach(sale -> sales.put(sale.number(), sale));
        lastNumber = state.lastNumber();
    }

    public record State(List<SaleRecord> sales, long lastNumber) implements Serializable {
    }
}
