package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.settings.adapter.InMemoryCurrencySettingsRepository;
import com.sistemaventas.settings.application.ManageCurrency;
import java.util.Currency;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class SettingsDialogTest {
    @Test
    void loadsDefaultCurrencyValidatesAndSavesAsynchronouslyWithOneRefresh() throws Exception {
        InMemoryCurrencySettingsRepository repository = new InMemoryCurrencySettingsRepository(); CatalogRefreshCoordinator refresh = new CatalogRefreshCoordinator(); AtomicInteger refreshes = new AtomicInteger(); refresh.subscribe(refreshes::incrementAndGet);
        CountDownLatch loaded = new CountDownLatch(1); CountDownLatch done = new CountDownLatch(1);
        SettingsDialog initialSettings = new SettingsDialog(new ManageCurrency(repository), refresh, loaded::countDown, done::countDown);
        assertTrue(loaded.await(2, TimeUnit.SECONDS)); onEdt(() -> { assertEquals("ARS", initialSettings.currentCurrency().getText()); assertTrue(java.util.Arrays.stream(initialSettings.getComponents()).filter(JLabel.class::isInstance).map(JLabel.class::cast).anyMatch(label -> "ISO-4217 currency code".equals(label.getText()) && label.getLabelFor() == initialSettings.currencyInput())); assertEquals(1, java.util.Arrays.stream(initialSettings.getComponents()).filter(JLabel.class::isInstance).map(JLabel.class::cast).filter(label -> label.getLabelFor() != null).count()); initialSettings.currencyInput().setText("bad"); initialSettings.saveButton().doClick(); });
        assertTrue(done.await(2, TimeUnit.SECONDS)); onEdt(() -> assertEquals("Currency must be a valid ISO-4217 code.", initialSettings.feedback().getText())); assertEquals(Optional.of(Currency.getInstance("ARS")), repository.load());
        onEdt(() -> { initialSettings.currencyInput().setText("USD"); initialSettings.cancelButton().doClick(); }); assertEquals(Optional.of(Currency.getInstance("ARS")), repository.load());
        CountDownLatch reloaded = new CountDownLatch(1); CountDownLatch saved = new CountDownLatch(1); SettingsDialog finalSettings = new SettingsDialog(new ManageCurrency(repository), refresh, reloaded::countDown, saved::countDown); assertTrue(reloaded.await(2, TimeUnit.SECONDS));
        onEdt(() -> { finalSettings.currencyInput().setText("eur"); finalSettings.saveButton().doClick(); }); assertTrue(saved.await(2, TimeUnit.SECONDS)); assertEquals(Optional.of(Currency.getInstance("EUR")), repository.load()); assertEquals(1, refreshes.get());
        CountDownLatch configuredLoaded = new CountDownLatch(1); SettingsDialog configured = new SettingsDialog(new ManageCurrency(repository), refresh, configuredLoaded::countDown, () -> { }); assertTrue(configuredLoaded.await(2, TimeUnit.SECONDS)); onEdt(() -> assertEquals("EUR", configured.currentCurrency().getText()));
    }

    private static void onEdt(ThrowingRunnable action) throws Exception { SwingUtilities.invokeAndWait(() -> { try { action.run(); } catch (Exception exception) { throw new RuntimeException(exception); } }); }
    private interface ThrowingRunnable { void run() throws Exception; }
}
