package com.sistemaventas.sales.port;

import com.sistemaventas.sales.application.SaleRecord;
import java.util.List;
import java.util.Optional;

public interface SalesPort {

    long nextNumber();

    void save(SaleRecord sale);

    List<SaleRecord> list();

    Optional<SaleRecord> find(long number);
}
