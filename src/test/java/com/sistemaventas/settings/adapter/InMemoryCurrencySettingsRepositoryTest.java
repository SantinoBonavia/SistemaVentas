package com.sistemaventas.settings.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class InMemoryCurrencySettingsRepositoryTest {

    @Test
    void startsEachFreshSessionWithArgentinePesosAndKeepsInstancesIndependent() {
        InMemoryCurrencySettingsRepository populated = new InMemoryCurrencySettingsRepository();
        populated.save(Currency.getInstance("USD"));
        InMemoryCurrencySettingsRepository fresh = new InMemoryCurrencySettingsRepository();

        assertEquals(Optional.of(Currency.getInstance("USD")), populated.load());
        assertEquals(Optional.of(Currency.getInstance("ARS")), fresh.load());
    }

    @Test
    void concurrentSavesLeaveExactlyOneValidCurrency() throws Exception {
        InMemoryCurrencySettingsRepository repository = new InMemoryCurrencySettingsRepository();
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Callable<Void>> saves = new ArrayList<>();
            for (int index = 0; index < 30; index++) {
                Currency currency = Currency.getInstance(index % 2 == 0 ? "USD" : "EUR");
                saves.add(() -> {
                    repository.save(currency);
                    return null;
                });
            }

            List<Future<Void>> results = executor.invokeAll(saves);

            assertEquals(30, results.size());
            assertEquals(1, repository.load().stream().count());
            assertTrue(List.of(Currency.getInstance("USD"), Currency.getInstance("EUR"))
                    .contains(repository.load().orElseThrow()));
        } finally {
            executor.shutdownNow();
        }
    }
}
