package com.sistemaventas.sales;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.BrowseProducts;
import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.sales.adapter.InMemorySalesRepository;
import com.sistemaventas.sales.application.Buyer;
import com.sistemaventas.sales.application.CancelSale;
import com.sistemaventas.sales.application.Cart;
import com.sistemaventas.sales.application.PaymentMethod;
import com.sistemaventas.sales.application.Pesos;
import com.sistemaventas.sales.application.RegisterSale;
import com.sistemaventas.sales.application.SaleRecord;
import com.sistemaventas.sales.application.SaleTotals;
import com.sistemaventas.sales.application.SalesReport;
import com.sistemaventas.sales.application.TicketFormatter;
import com.sistemaventas.store.StoreProfile;
import com.sistemaventas.tax.CondicionIva;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SalesTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-24T18:30:00Z"), ZoneId.of("America/Argentina/Buenos_Aires"));
    private InMemoryProductRegistrationRepository products;
    private InMemorySalesRepository sales;
    private RegisterSale registerSale;
    private ProductView hammer;
    private ProductView drill;
    private ProductView capitalGood;

    @BeforeEach
    void setUp() {
        products = new InMemoryProductRegistrationRepository();
        sales = new InMemorySalesRepository();
        RegisterProduct register = new RegisterProduct(products);
        register.register(new ProductRegistrationCommand("Martillo", "18.900", "Manuales", "5", "MAR-001", "21", "2", ""));
        register.register(new ProductRegistrationCommand("Taladro", "98.500", "Eléctricas", "1", "TAL-100", "21", "1", ""));
        register.register(new ProductRegistrationCommand("Hormigonera", "1.105.000", "Máquinas", "2", "HOR-1", "10,5", "0", ""));
        List<ProductView> all = new BrowseProducts(products).list();
        hammer = all.get(0);
        drill = all.get(1);
        capitalGood = all.get(2);
        registerSale = new RegisterSale(products, sales, () -> StoreProfile.DEFAULT, CLOCK);
    }

    @Test
    void cartNeverExceedsKnownStockAndReportsWhy() {
        Cart cart = new Cart();

        assertEquals(Optional.empty(), cart.add(drill));
        assertEquals(Optional.of("Solo hay 1 unidades de \"Taladro\"."), cart.add(drill));
        assertEquals(1, cart.quantityOf(drill.id()));
        cart.setQuantity(hammer, 3);
        assertEquals(4, cart.itemCount());
        cart.setQuantity(hammer, 0);
        assertEquals(1, cart.itemCount());
    }

    @Test
    void totalsSplitNetAndIvaPerRateAndApplyTheDiscountBeforeDiscriminating() {
        Cart cart = new Cart();
        cart.setQuantity(hammer, 2);
        cart.add(capitalGood);
        cart.setDiscountPercent(new BigDecimal("10"));

        SaleTotals totals = cart.totals();

        assertEquals(new BigDecimal("1142800.00"), totals.subtotal());
        assertEquals(new BigDecimal("114280.00"), totals.discount());
        assertEquals(new BigDecimal("1028520.00"), totals.total());
        assertEquals(2, totals.breakdown().size());
        assertEquals(new BigDecimal("21"), totals.breakdown().get(0).ivaPercent());
        assertEquals(new BigDecimal("34020.00"), totals.breakdown().get(0).net().add(totals.breakdown().get(0).iva()));
        assertEquals(totals.total(), totals.netTotal().add(totals.ivaTotal()));
    }

    @Test
    void checkoutDiscountsStockNumbersTheTicketAndComputesCashChange() {
        Cart cart = new Cart();
        cart.setQuantity(hammer, 2);

        RegisterSale.Result result = registerSale.checkout(new RegisterSale.Checkout(cart.lines(), BigDecimal.ZERO,
                PaymentMethod.EFECTIVO, Buyer.CONSUMIDOR_FINAL, new BigDecimal("40000")));

        SaleRecord sale = assertInstanceOf(RegisterSale.Result.Completed.class, result).sale();
        assertEquals("00001-00000001", sale.formattedNumber());
        assertEquals("B", sale.letter());
        assertEquals(new BigDecimal("37800.00"), sale.totals().total());
        assertEquals(new BigDecimal("2200.00"), sale.change());
        assertEquals(3, products.findProductById(hammer.id()).orElseThrow().stock());
        assertEquals(2, sales.nextNumber());
    }

    @Test
    void rejectsTheWholeSaleWithoutTouchingStockWhenAnyLineIsShort() throws Exception {
        Cart cart = new Cart();
        cart.setQuantity(hammer, 2);
        cart.add(drill);
        products.applyStockChanges(java.util.Map.of(drill.id(), -1));

        RegisterSale.Result result = registerSale.checkout(new RegisterSale.Checkout(cart.lines(), BigDecimal.ZERO,
                PaymentMethod.DEBITO, null, null));

        assertEquals("Stock insuficiente de \"Taladro\": quedan 0 unidades.",
                assertInstanceOf(RegisterSale.Result.Rejected.class, result).message());
        assertEquals(5, products.findProductById(hammer.id()).orElseThrow().stock());
        assertTrue(sales.list().isEmpty());
    }

    @Test
    void requiresAValidCuitForFacturaAAndInsufficientCashIsRejected() {
        Cart cart = new Cart();
        cart.add(hammer);

        RegisterSale.Result invalidCuit = registerSale.checkout(new RegisterSale.Checkout(cart.lines(), BigDecimal.ZERO,
                PaymentMethod.TRANSFERENCIA, new Buyer(CondicionIva.RESPONSABLE_INSCRIPTO, "Obras SRL", "30-1"), null));
        RegisterSale.Result shortCash = registerSale.checkout(new RegisterSale.Checkout(cart.lines(), BigDecimal.ZERO,
                PaymentMethod.EFECTIVO, null, new BigDecimal("10000")));
        RegisterSale.Result facturaA = registerSale.checkout(new RegisterSale.Checkout(cart.lines(), BigDecimal.ZERO,
                PaymentMethod.TRANSFERENCIA, new Buyer(CondicionIva.RESPONSABLE_INSCRIPTO, "Obras SRL", "30712345671"), null));

        assertInstanceOf(RegisterSale.Result.Rejected.class, invalidCuit);
        assertEquals("El efectivo recibido no alcanza para cubrir el total.",
                assertInstanceOf(RegisterSale.Result.Rejected.class, shortCash).message());
        SaleRecord sale = assertInstanceOf(RegisterSale.Result.Completed.class, facturaA).sale();
        assertEquals("A", sale.letter());
        assertEquals("30-71234567-1", sale.buyer().cuit());
        assertNull(sale.change());
    }

    @Test
    void cancellingReturnsStockAndExcludesTheSaleFromTotals() {
        Cart cart = new Cart();
        cart.setQuantity(hammer, 2);
        SaleRecord sale = ((RegisterSale.Result.Completed) registerSale.checkout(new RegisterSale.Checkout(cart.lines(),
                BigDecimal.ZERO, PaymentMethod.CREDITO, null, null))).sale();
        CancelSale cancelSale = new CancelSale(products, sales);

        assertInstanceOf(RegisterSale.Result.Completed.class, cancelSale.cancel(sale.number()));
        assertInstanceOf(RegisterSale.Result.Rejected.class, cancelSale.cancel(sale.number()));

        assertEquals(5, products.findProductById(hammer.id()).orElseThrow().stock());
        SalesReport.Summary today = new SalesReport(sales).between(LocalDate.of(2026, 10, 24), null);
        assertEquals(1, today.sales().size());
        assertEquals(0, today.count());
        assertEquals(BigDecimal.ZERO, today.total());
    }

    @Test
    void reportSummarisesTotalsIvaPaymentMethodsAndBestSellers() {
        Cart first = new Cart();
        first.setQuantity(hammer, 2);
        registerSale.checkout(new RegisterSale.Checkout(first.lines(), BigDecimal.ZERO, PaymentMethod.EFECTIVO, null, null));
        Cart second = new Cart();
        second.add(hammer);
        second.add(drill);
        registerSale.checkout(new RegisterSale.Checkout(second.lines(), BigDecimal.ZERO, PaymentMethod.QR, null, null));

        SalesReport.Summary summary = new SalesReport(sales).between(null, null);

        assertEquals(2, summary.count());
        assertEquals(new BigDecimal("155200.00"), summary.total());
        assertEquals(new BigDecimal("77600.00"), summary.average());
        assertEquals(new BigDecimal("37800.00"), summary.byPayment().get(PaymentMethod.EFECTIVO));
        assertEquals("Martillo", summary.topProducts().getFirst().name());
        assertEquals(3, summary.topProducts().getFirst().quantity());
        assertTrue(summary.iva().signum() > 0);
    }

    @Test
    void ticketShowsContainedIvaForConsumidorFinalAndDiscriminatesForFacturaA() {
        Cart cart = new Cart();
        cart.add(hammer);
        SaleRecord b = ((RegisterSale.Result.Completed) registerSale.checkout(new RegisterSale.Checkout(cart.lines(),
                BigDecimal.ZERO, PaymentMethod.EFECTIVO, null, new BigDecimal("20000")))).sale();
        SaleRecord a = ((RegisterSale.Result.Completed) registerSale.checkout(new RegisterSale.Checkout(cart.lines(),
                BigDecimal.ZERO, PaymentMethod.TRANSFERENCIA,
                new Buyer(CondicionIva.RESPONSABLE_INSCRIPTO, "Obras SRL", "30712345671"), null))).sale();

        String ticketB = TicketFormatter.format(b, StoreProfile.DEFAULT);
        String ticketA = TicketFormatter.format(a, StoreProfile.DEFAULT);

        assertTrue(ticketB.contains("COMPROBANTE \"B\""));
        assertTrue(ticketB.contains("Ley 27.743"));
        assertTrue(ticketB.contains("$3.280,17"));
        assertTrue(ticketB.contains("Vuelto"));
        assertTrue(ticketA.contains("Neto gravado 21%"));
        assertTrue(ticketA.contains("$15.619,83"));
        assertTrue(ticketA.lines().allMatch(line -> line.length() <= TicketFormatter.WIDTH));
    }

    @Test
    void formatsArgentinePesos() {
        assertEquals("$18.900,00", Pesos.format(new BigDecimal("18900")));
        assertEquals("$18.900", Pesos.compact(new BigDecimal("18900.00")));
        assertEquals("$1.234,50", Pesos.compact(new BigDecimal("1234.5")));
        assertEquals("-$10,00", Pesos.format(new BigDecimal("-10")));
        assertEquals("1.250.000,00", Pesos.plain(new BigDecimal("1250000")));
    }
}
