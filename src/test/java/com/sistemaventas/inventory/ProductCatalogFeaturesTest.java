package com.sistemaventas.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.BrowseProducts;
import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.application.ReceiveStock;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.inventory.application.RegistrationResult;
import com.sistemaventas.inventory.application.UpdateProduct;
import com.sistemaventas.inventory.application.UpdateProductCommand;
import com.sistemaventas.inventory.application.UpdateResult;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductCatalogFeaturesTest {
    private final InMemoryProductRegistrationRepository repository = new InMemoryProductRegistrationRepository();
    private final RegisterProduct register = new RegisterProduct(repository);

    @Test
    void storesCodeIvaRateMinimumStockAndImageAndDerivesNetPrice() {
        register.register(new ProductRegistrationCommand("Hormigonera", "1.105.000", "Máquinas", "2", "hor-1", "10,5", "1", "art:generico"));

        ProductView product = new BrowseProducts(repository).list().getFirst();

        assertEquals("HOR-1", product.code());
        assertEquals(new BigDecimal("10.5"), product.ivaPercent());
        assertEquals(new BigDecimal("1000000.00"), product.netPrice());
        assertEquals(new BigDecimal("105000.00"), product.ivaAmount());
        assertEquals(1, product.minStock());
        assertEquals("art:generico", product.image());
        assertEquals(ProductView.StockStatus.DISPONIBLE, product.stockStatus());
    }

    @Test
    void rejectsDuplicateCodesUnknownRatesAndInvalidMinimumStock() {
        register.register(new ProductRegistrationCommand("Martillo", "18.900", "Manuales", "2", "MAR-001", "21", "1", ""));

        assertEquals("code", ((RegistrationResult.ValidationFailed) register.register(
                new ProductRegistrationCommand("Otro", "100", "Manuales", "1", "mar-001", "21", "0", ""))).field());
        assertEquals("iva", ((RegistrationResult.ValidationFailed) register.register(
                new ProductRegistrationCommand("Otro", "100", "Manuales", "1", "", "19", "0", ""))).field());
        assertEquals("minStock", ((RegistrationResult.ValidationFailed) register.register(
                new ProductRegistrationCommand("Otro", "100", "Manuales", "1", "", "21", "-2", ""))).field());
    }

    @Test
    void updateKeepsItsOwnCodeButRejectsAnotherProductsCode() {
        register.register(new ProductRegistrationCommand("Martillo", "18.900", "Manuales", "2", "MAR-001", "21", "1", ""));
        register.register(new ProductRegistrationCommand("Pinza", "13.700", "Manuales", "2", "PIN-020", "21", "1", ""));
        UpdateProduct update = new UpdateProduct(repository);

        assertInstanceOf(UpdateResult.Updated.class,
                update.update(new UpdateProductCommand(1, "Martillo", "19.500", "Manuales", "2", "MAR-001", "21", "1", "")));
        assertInstanceOf(UpdateResult.ValidationFailed.class,
                update.update(new UpdateProductCommand(1, "Martillo", "19.500", "Manuales", "2", "PIN-020", "21", "1", "")));
    }

    @Test
    void receivesAndAdjustsStockWithoutGoingNegative() {
        register.register(new ProductRegistrationCommand("Martillo", "18.900", "Manuales", "2", "", "21", "3", ""));
        ReceiveStock receive = new ReceiveStock(repository, repository);

        ProductView received = ((ReceiveStock.Result.Done) receive.adjust(1, 10)).product();
        ReceiveStock.Result tooMany = receive.adjust(1, -50);

        assertEquals(12, received.stock());
        assertEquals("Stock insuficiente de \"Martillo\": quedan 12 unidades.", ((ReceiveStock.Result.Rejected) tooMany).message());
        assertInstanceOf(ReceiveStock.Result.Rejected.class, receive.adjust(1, 0));
        assertEquals(ProductView.StockStatus.BAJO, ((ReceiveStock.Result.Done) receive.adjust(1, -10)).product().stockStatus());
    }
}
