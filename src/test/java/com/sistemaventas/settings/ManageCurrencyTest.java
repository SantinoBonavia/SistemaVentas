package com.sistemaventas.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.port.ProductRegistrationPort.ProductDetails;
import com.sistemaventas.settings.adapter.InMemoryCurrencySettingsRepository;
import com.sistemaventas.settings.application.ManageCurrency;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ManageCurrencyTest {

    @Test
    void startsWithArgentinePesosAndSavesNormalizedIsoCurrencyCodes() {
        ManageCurrency manageCurrency = new ManageCurrency(new InMemoryCurrencySettingsRepository());

        assertEquals(Optional.of(Currency.getInstance("ARS")), manageCurrency.load());
        assertInstanceOf(ManageCurrency.Result.Saved.class, manageCurrency.save(" usd "));
        assertEquals(Optional.of(Currency.getInstance("USD")), manageCurrency.load());
    }

    @Test
    void replacesTheSessionCurrencyWithoutAffectingStoredProductAmounts() throws Exception {
        InMemoryCurrencySettingsRepository settings = new InMemoryCurrencySettingsRepository();
        ManageCurrency manageCurrency = new ManageCurrency(settings);
        InMemoryProductRegistrationRepository products = new InMemoryProductRegistrationRepository();
        products.createProductUsingExistingOrNewCategory(
                new ProductDetails("Coffee", new BigDecimal("12.50"), "Beverages", 0));

        manageCurrency.save("USD");
        assertInstanceOf(ManageCurrency.Result.Saved.class, manageCurrency.save("EUR"));

        assertEquals(Optional.of(Currency.getInstance("EUR")), manageCurrency.load());
        ProductView product = products.listProducts().getFirst();
        assertEquals(new BigDecimal("12.50"), product.price());
        assertEquals(2, product.price().scale());
    }

    @Test
    void rejectsBlankAndUnknownCodesLegiblyWithoutReplacingTheCurrentCurrency() {
        ManageCurrency manageCurrency = new ManageCurrency(new InMemoryCurrencySettingsRepository());
        manageCurrency.save("USD");

        assertInvalid(manageCurrency.save("  "));
        assertInvalid(manageCurrency.save("not-a-currency"));

        assertEquals(Optional.of(Currency.getInstance("USD")), manageCurrency.load());
    }

    @Test
    void retainingThePriorValueRequiresNoSaveWhenTheCallerCancels() {
        ManageCurrency manageCurrency = new ManageCurrency(new InMemoryCurrencySettingsRepository());
        manageCurrency.save("EUR");

        assertEquals(Optional.of(Currency.getInstance("EUR")), manageCurrency.load());
    }

    private void assertInvalid(ManageCurrency.Result result) {
        ManageCurrency.Result.ValidationFailed invalid = assertInstanceOf(
                ManageCurrency.Result.ValidationFailed.class, result);
        assertEquals("Currency must be a valid ISO-4217 code.", invalid.message());
    }
}
