package com.sistemaventas.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.BrowseProducts;
import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.sales.adapter.InMemorySalesRepository;
import com.sistemaventas.store.StoreProfile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DataStoreTest {

    @Test
    void roundTripsProductsSalesAndStoreProfile(@TempDir Path directory) throws IOException {
        InMemoryProductRegistrationRepository products = new InMemoryProductRegistrationRepository();
        new RegisterProduct(products).register(new ProductRegistrationCommand("Candado", "11.400", "Seguridad", "10",
                "CND-620", "21", "4", "art:candado"));
        DataStore store = new DataStore(directory);
        assertTrue(store.load().isEmpty());

        store.save(new DataStore.Snapshot(products.exportState(), new InMemorySalesRepository().exportState(), StoreProfile.DEFAULT));

        DataStore.Snapshot loaded = store.load().orElseThrow();
        InMemoryProductRegistrationRepository restored = new InMemoryProductRegistrationRepository();
        restored.importState(loaded.inventory());
        assertEquals(new BrowseProducts(products).list(), new BrowseProducts(restored).list());
        assertTrue(restored.productCodeExists("cnd-620", 0));
        assertEquals(StoreProfile.DEFAULT, loaded.store());
        assertFalse(Files.exists(directory.resolve("datos.bin.tmp")));
    }

    @Test
    void reportsAnUnreadableFileInsteadOfSilentlyStartingEmpty(@TempDir Path directory) throws IOException {
        Files.writeString(directory.resolve("datos.bin"), "no es un archivo de datos");

        assertThrows(IOException.class, () -> new DataStore(directory).load());
    }
}
