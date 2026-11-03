package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.BrowseProducts;
import com.sistemaventas.inventory.application.DeleteProduct;
import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.application.ReceiveStock;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.inventory.application.UpdateProduct;
import com.sistemaventas.sales.adapter.InMemorySalesRepository;
import com.sistemaventas.sales.application.CancelSale;
import com.sistemaventas.sales.application.PaymentMethod;
import com.sistemaventas.sales.application.RegisterSale;
import com.sistemaventas.sales.application.SalesReport;
import com.sistemaventas.store.StoreSettings;
import com.sistemaventas.tax.AlicuotaIva;
import com.sistemaventas.tax.CondicionIva;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Recorre las pantallas sin mostrar ventanas, como lo haría un cajero. */
class UiFlowTest {
    private InMemoryProductRegistrationRepository products;
    private InMemorySalesRepository sales;
    private RecordingDialogs dialogs;
    private Services services;

    @BeforeEach
    void setUp() {
        products = new InMemoryProductRegistrationRepository();
        sales = new InMemorySalesRepository();
        dialogs = new RecordingDialogs();
        RegisterProduct register = new RegisterProduct(products);
        register.register(new ProductRegistrationCommand("Martillo carpintero", "18.900", "Manuales", "5", "MAR-001", "21", "2", "art:martillo"));
        register.register(new ProductRegistrationCommand("Amoladora", "86.900", "Eléctricas", "0", "AMO-110", "21", "2", "art:amoladora"));
        StoreSettings store = new StoreSettings();
        services = new Services(new BrowseProducts(products), register, new UpdateProduct(products), new DeleteProduct(products),
                new ReceiveStock(products, products), new RegisterSale(products, sales, store::profile, Clock.systemDefaultZone()),
                new CancelSale(products, sales), new SalesReport(sales), store, products::categoryNames,
                Path.of(System.getProperty("java.io.tmpdir")), null, new CatalogRefreshCoordinator(), dialogs,
                Clock.systemDefaultZone());
    }

    @Test
    void sellsByScanningACodeAndChargingCashWithChange() throws Exception {
        onEdt(() -> {
            MainFrame main = new MainFrame(services);
            PosPanel pos = main.pos();
            assertEquals(2, pos.cards().size());
            assertFalse(pos.chargeButton().isEnabled());

            pos.searchField().setText("mar-001");
            pos.searchField().postActionEvent();
            pos.searchField().setText("MAR-001");
            pos.searchField().postActionEvent();
            assertEquals(2, pos.cart().quantityOf(1));
            assertEquals("$37.800,00", pos.totalLabel().getText());

            pos.paidWithField().setText("40.000");
            assertEquals("Vuelto $2.200,00", pos.changeLabel().getText());
            pos.chargeButton().doClick();

            assertTrue(pos.cart().isEmpty());
            assertEquals(3, products.findProductById(1).orElseThrow().stock());
            assertEquals(1, sales.list().size());
            assertEquals("Hoy $37.800", main.todayLabel().getText());
            TicketPanel ticket = assertInstanceOf(TicketPanel.class, dialogs.shown.getLast());
            assertTrue(ticket.text().contains("Vuelto"));
        });
    }

    @Test
    void outOfStockProductsCannotBeAddedAndFacturaANeedsACuit() throws Exception {
        onEdt(() -> {
            PosPanel pos = new PosPanel(services);
            ProductView grinder = products.findProductById(2).orElseThrow();
            pos.addToCart(grinder);
            assertTrue(pos.cart().isEmpty());
            assertEquals("\"Amoladora\" no tiene stock.", pos.feedback().getText());

            pos.addToCart(products.findProductById(1).orElseThrow());
            pos.buyerTypeBox().setSelectedItem(CondicionIva.RESPONSABLE_INSCRIPTO);
            pos.selectPayment(PaymentMethod.TRANSFERENCIA);
            pos.chargeButton().doClick();
            assertEquals("Ingresá un CUIT válido del cliente para emitir el comprobante.", pos.feedback().getText());

            pos.buyerCuitField().setText("30-71234567-1");
            pos.buyerNameField().setText("Constructora del Litoral SA");
            pos.chargeButton().doClick();
            assertEquals("A", sales.list().getFirst().letter());
        });
    }

    @Test
    void productFormComputesIvaLiveAndSavesANewProduct() throws Exception {
        onEdt(() -> {
            List<Boolean> closed = new ArrayList<>();
            ProductFormPanel form = new ProductFormPanel(services, null, () -> closed.add(true));
            form.nameInput().setText("Hormigonera 130 L");
            form.codeInput().setText("HOR-130");
            form.categoryInput().getEditor().setItem("Máquinas");
            form.ivaInput().setSelectedItem(AlicuotaIva.REDUCIDA);
            form.netInput().setText("1.000.000");
            assertEquals("1.105.000,00", form.finalPriceInput().getText());
            assertEquals("$105.000,00", form.ivaAmountLabel().getText());

            form.ivaInput().setSelectedItem(AlicuotaIva.GENERAL);
            assertEquals("1.105.000,00", form.finalPriceInput().getText());
            assertEquals("913.223,14", form.netInput().getText());

            form.stockInput().setText("2");
            form.saveButton().doClick();

            assertEquals(List.of(true), closed);
            ProductView saved = products.findProductById(3).orElseThrow();
            assertEquals("HOR-130", saved.code());
            assertEquals(AlicuotaIva.GENERAL.percent(), saved.ivaPercent());
        });
    }

    @Test
    void productFormShowsValidationErrorsInSpanish() throws Exception {
        onEdt(() -> {
            ProductFormPanel form = new ProductFormPanel(services, null, () -> { });
            form.nameInput().setText("Sin precio");
            form.categoryInput().getEditor().setItem("Varios");
            form.saveButton().doClick();
            assertEquals("El precio debe ser un importe positivo con hasta dos decimales.", form.feedback().getText());
        });
    }

    @Test
    void inventoryFiltersLowStockAndCancelledSaleRestoresStock() throws Exception {
        onEdt(() -> {
            InventoryPanel inventory = new InventoryPanel(services);
            SalesPanel salesPanel = new SalesPanel(services);
            assertEquals(2, inventory.visibleProducts().size());
            inventory.lowOnlyFilter().doClick();
            assertEquals(List.of("Amoladora"), inventory.visibleProducts().stream().map(ProductView::name).toList());

            PosPanel pos = new PosPanel(services);
            pos.addToCart(products.findProductById(1).orElseThrow());
            pos.chargeButton().doClick();
            assertEquals("1", salesPanel.countStat().getText());
            assertTrue(salesPanel.ticketText().contains("Martillo carpintero"));

            salesPanel.cancelButton().doClick();
            assertEquals(5, products.findProductById(1).orElseThrow().stock());
            assertEquals("0", salesPanel.countStat().getText());
            assertFalse(salesPanel.cancelButton().isEnabled());
        });
    }

    @Test
    void settingsValidateCuitAndNavigationFollowsSections() throws Exception {
        onEdt(() -> {
            SettingsPanel settings = new SettingsPanel(services);
            settings.cuitInput().setText("30-71234567-2");
            settings.save();
            assertEquals("El CUIT no es válido. Revisá los 11 dígitos.", settings.feedback().getText());
            settings.cuitInput().setText("30712345671");
            settings.save();
            assertEquals("30-71234567-1", services.store().profile().cuit());

            MainFrame main = new MainFrame(services);
            main.navButton(MainFrame.Section.VENTAS).doClick();
            assertEquals(MainFrame.Section.VENTAS, main.current());
            main.show(MainFrame.Section.VENDER);
            assertTrue(main.navButton(MainFrame.Section.VENDER).isSelected());
        });
    }

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static final class RecordingDialogs implements Dialogs {
        private final List<JComponent> shown = new ArrayList<>();

        @Override
        public void show(String title, Function<Runnable, JComponent> content) {
            shown.add(content.apply(() -> { }));
        }

        @Override
        public boolean confirm(String title, String message, String confirmLabel) {
            return true;
        }

        @Override
        public void message(String title, String message) {
        }

        @Override
        public Optional<Path> choosePhoto() {
            return Optional.empty();
        }
    }
}
