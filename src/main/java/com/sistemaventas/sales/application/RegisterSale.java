package com.sistemaventas.sales.application;

import com.sistemaventas.inventory.port.StockPort;
import com.sistemaventas.inventory.port.StockPort.StockException;
import com.sistemaventas.sales.port.SalesPort;
import com.sistemaventas.store.StoreProfile;
import com.sistemaventas.tax.CondicionIva;
import com.sistemaventas.tax.Cuit;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Cobra una venta: valida el comprobante, descuenta el stock y registra el comprobante. */
public final class RegisterSale {
    private final StockPort stock;
    private final SalesPort sales;
    private final Supplier<StoreProfile> store;
    private final Clock clock;

    public RegisterSale(StockPort stock, SalesPort sales, Supplier<StoreProfile> store, Clock clock) {
        this.stock = stock;
        this.sales = sales;
        this.store = store;
        this.clock = clock;
    }

    public record Checkout(List<SaleLine> lines, BigDecimal discountPercent, PaymentMethod payment, Buyer buyer,
                           BigDecimal paidWith) {
    }

    public sealed interface Result {
        record Completed(SaleRecord sale) implements Result {
        }

        record Rejected(String message) implements Result {
        }
    }

    public synchronized Result checkout(Checkout checkout) {
        if (checkout == null || checkout.lines() == null || checkout.lines().isEmpty()) {
            return new Result.Rejected("Agregá al menos un producto al ticket.");
        }
        if (checkout.payment() == null) {
            return new Result.Rejected("Elegí un medio de pago.");
        }
        Buyer buyer = checkout.buyer() == null ? Buyer.CONSUMIDOR_FINAL : checkout.buyer();
        if (buyer.condicion() != CondicionIva.CONSUMIDOR_FINAL) {
            if (Cuit.normalize(buyer.cuit()).isEmpty()) {
                return new Result.Rejected("Ingresá un CUIT válido del cliente para emitir el comprobante.");
            }
            if (buyer.name().isEmpty()) {
                return new Result.Rejected("Ingresá la razón social del cliente.");
            }
            buyer = new Buyer(buyer.condicion(), buyer.name(), Cuit.normalize(buyer.cuit()).orElseThrow());
        }

        SaleTotals totals = SaleTotals.of(checkout.lines(), checkout.discountPercent());
        BigDecimal paidWith = null;
        BigDecimal change = null;
        if (checkout.payment() == PaymentMethod.EFECTIVO && checkout.paidWith() != null) {
            if (checkout.paidWith().compareTo(totals.total()) < 0) {
                return new Result.Rejected("El efectivo recibido no alcanza para cubrir el total.");
            }
            paidWith = checkout.paidWith();
            change = paidWith.subtract(totals.total());
        }

        Map<Long, Integer> deltas = new LinkedHashMap<>();
        checkout.lines().forEach(line -> deltas.merge(line.productId(), -line.quantity(), Integer::sum));
        try {
            stock.applyStockChanges(deltas);
        } catch (StockException exception) {
            return new Result.Rejected(exception.getMessage());
        }

        StoreProfile profile = store.get();
        String letter = CondicionIva.comprobanteLetter(profile.condicion(), buyer.condicion());
        SaleRecord sale = new SaleRecord(sales.nextNumber(), profile.pointOfSale(), letter,
                LocalDateTime.now(clock).withNano(0), checkout.lines(), totals, checkout.payment(), buyer,
                paidWith, change, false);
        sales.save(sale);
        return new Result.Completed(sale);
    }
}
