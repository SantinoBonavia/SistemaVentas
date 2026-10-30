package com.sistemaventas;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.BrowseProducts;
import com.sistemaventas.inventory.application.DeleteProduct;
import com.sistemaventas.inventory.application.ReceiveStock;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.inventory.application.UpdateProduct;
import com.sistemaventas.persistence.DataStore;
import com.sistemaventas.sales.adapter.InMemorySalesRepository;
import com.sistemaventas.sales.application.CancelSale;
import com.sistemaventas.sales.application.RegisterSale;
import com.sistemaventas.sales.application.SalesReport;
import com.sistemaventas.store.StoreSettings;
import com.sistemaventas.ui.CatalogRefreshCoordinator;
import com.sistemaventas.ui.Dialogs;
import com.sistemaventas.ui.MainFrame;
import com.sistemaventas.ui.Services;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Optional;
import java.util.function.Function;
import javax.swing.JComponent;

/**
 * Arma la aplicación. Sin {@link DataStore} todo vive en memoria (pruebas); con uno, carga los datos
 * guardados y los vuelve a guardar después de cada cambio.
 */
final class ApplicationComposition {
    private final InMemoryProductRegistrationRepository products;
    private final InMemorySalesRepository sales;
    private final Services services;
    private final MainFrame mainFrame;

    private ApplicationComposition(InMemoryProductRegistrationRepository products, InMemorySalesRepository sales,
                                   Services services, MainFrame mainFrame) {
        this.products = products;
        this.sales = sales;
        this.services = services;
        this.mainFrame = mainFrame;
    }

    /** Sesión vacía en memoria, sin ventanas: segura para pruebas sin pantalla. */
    static ApplicationComposition create() {
        return create(new NoDialogs(), null, Clock.systemDefaultZone(), false);
    }

    static ApplicationComposition create(Dialogs dialogs, DataStore store, Clock clock, boolean seedWhenEmpty) {
        InMemoryProductRegistrationRepository products = new InMemoryProductRegistrationRepository();
        InMemorySalesRepository sales = new InMemorySalesRepository();
        StoreSettings storeSettings = new StoreSettings();
        boolean restored = store != null && restore(store, products, sales, storeSettings);

        RegisterProduct registerProduct = new RegisterProduct(products);
        if (!restored && seedWhenEmpty) {
            DemoCatalog.load(registerProduct);
        }
        if (store != null) {
            Runnable save = () -> save(store, products, sales, storeSettings);
            products.onChange(save);
            sales.onChange(save);
            storeSettings.onChange(save);
            if (!restored) save.run();
        }

        CatalogRefreshCoordinator events = new CatalogRefreshCoordinator();
        Services services = new Services(new BrowseProducts(products), registerProduct, new UpdateProduct(products),
                new DeleteProduct(products), new ReceiveStock(products, products),
                new RegisterSale(products, sales, storeSettings::profile, clock), new CancelSale(products, sales),
                new SalesReport(sales), storeSettings, products::categoryNames,
                store == null ? Path.of(System.getProperty("java.io.tmpdir"), "sistemaventas-fotos") : store.photosDirectory(),
                store == null ? null : store.file(), events, dialogs, clock);
        return new ApplicationComposition(products, sales, services, new MainFrame(services));
    }

    Services services() { return services; }
    MainFrame mainFrame() { return mainFrame; }
    InMemoryProductRegistrationRepository products() { return products; }
    InMemorySalesRepository sales() { return sales; }

    private static boolean restore(DataStore store, InMemoryProductRegistrationRepository products,
                                   InMemorySalesRepository sales, StoreSettings settings) {
        try {
            Optional<DataStore.Snapshot> snapshot = store.load();
            snapshot.ifPresent(saved -> {
                products.importState(saved.inventory());
                sales.importState(saved.sales());
                settings.restore(saved.store());
            });
            return snapshot.isPresent();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudieron leer los datos guardados en " + store.file()
                    + ". Revisá el archivo o movelo para empezar de cero.", exception);
        }
    }

    private static void save(DataStore store, InMemoryProductRegistrationRepository products,
                             InMemorySalesRepository sales, StoreSettings settings) {
        try {
            store.save(new DataStore.Snapshot(products.exportState(), sales.exportState(), settings.profile()));
        } catch (IOException exception) {
            System.err.println("No se pudieron guardar los datos en " + store.file() + ": " + exception.getMessage());
        }
    }

    /** Diálogos inertes para pruebas: confirman todo y no muestran nada. */
    static final class NoDialogs implements Dialogs {
        @Override
        public void show(String title, Function<Runnable, JComponent> content) {
            content.apply(() -> { });
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
