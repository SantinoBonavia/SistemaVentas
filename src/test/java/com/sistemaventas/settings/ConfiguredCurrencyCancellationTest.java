package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.settings.adapter.InMemoryCurrencySettingsRepository;
import com.sistemaventas.settings.application.ManageCurrency;
import java.util.Currency;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ConfiguredCurrencyCancellationTest {
    @Test
    void cancellingProposedEurReplacementPreservesConfiguredUsd() throws Exception {
        assertCancellationPreservesConfiguredCurrency("USD", "EUR");
    }

    @Test
    void cancellingProposedArsReplacementPreservesConfiguredEur() throws Exception {
        assertCancellationPreservesConfiguredCurrency("EUR", "ARS");
    }

    private static void assertCancellationPreservesConfiguredCurrency(String configured, String proposed) throws Exception {
        InMemoryCurrencySettingsRepository repository = new InMemoryCurrencySettingsRepository();
        repository.save(Currency.getInstance(configured));
        CountDownLatch loaded = new CountDownLatch(1);
        SettingsDialog settings = new SettingsDialog(new ManageCurrency(repository), new CatalogRefreshCoordinator(),
                loaded::countDown, () -> { });

        assertTrue(loaded.await(2, TimeUnit.SECONDS));
        onEdt(() -> {
            assertEquals(configured, settings.currentCurrency().getText());
            settings.currencyInput().setText(proposed);
            settings.cancelButton().doClick();
        });

        assertEquals(Optional.of(Currency.getInstance(configured)), repository.load());
    }

    private static void onEdt(ThrowingRunnable action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        });
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
