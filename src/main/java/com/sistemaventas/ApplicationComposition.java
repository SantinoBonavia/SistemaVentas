package com.sistemaventas;

import com.sistemaventas.inventory.adapter.InMemoryProductRegistrationRepository;
import com.sistemaventas.inventory.application.BrowseProducts;
import com.sistemaventas.inventory.application.DeleteProduct;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.inventory.application.UpdateProduct;
import com.sistemaventas.settings.adapter.InMemoryCurrencySettingsRepository;
import com.sistemaventas.settings.application.ManageCurrency;
import com.sistemaventas.ui.CatalogRefreshCoordinator;
import com.sistemaventas.ui.MainFrame;
import com.sistemaventas.ui.ProductCatalogPanel;
import com.sistemaventas.ui.ProductDeletionDialog;
import com.sistemaventas.ui.ProductEditorDialog;
import com.sistemaventas.ui.ProductRegistrationPanel;
import com.sistemaventas.ui.SettingsDialog;
import java.util.Objects;
import java.util.function.Function;
import javax.swing.JPanel;

/** Database-free session composition, safe to construct in headless tests. */
final class ApplicationComposition {
    interface WindowHost {
        void show(String title, Function<Runnable, JPanel> content);
    }

    private final RegisterProduct registerProduct;
    private final BrowseProducts browseProducts;
    private final ManageCurrency currencySettings;
    private final MainFrame mainFrame;
    private final ProductWindows windows;

    private ApplicationComposition(RegisterProduct registerProduct, BrowseProducts browseProducts,
                                   ManageCurrency currencySettings, MainFrame mainFrame, ProductWindows windows) {
        this.registerProduct = registerProduct;
        this.browseProducts = browseProducts;
        this.currencySettings = currencySettings;
        this.mainFrame = mainFrame;
        this.windows = windows;
    }

    static ApplicationComposition create() {
        return create((title, content) -> { });
    }

    static ApplicationComposition create(WindowHost windowHost) {
        InMemoryProductRegistrationRepository products = new InMemoryProductRegistrationRepository();
        InMemoryCurrencySettingsRepository currencies = new InMemoryCurrencySettingsRepository();
        RegisterProduct registerProduct = new RegisterProduct(products);
        BrowseProducts browseProducts = new BrowseProducts(products);
        UpdateProduct updateProduct = new UpdateProduct(products);
        DeleteProduct deleteProduct = new DeleteProduct(products);
        ManageCurrency currencySettings = new ManageCurrency(currencies);
        CatalogRefreshCoordinator refresh = new CatalogRefreshCoordinator();
        ProductWindows windows = new ProductWindows(windowHost, updateProduct, deleteProduct, currencySettings, refresh);
        ProductRegistrationPanel registration = new ProductRegistrationPanel(registerProduct, refresh);
        ProductCatalogPanel catalog = new ProductCatalogPanel(browseProducts, refresh, currencySettings::load,
                windows::openEditor, windows::openDeletion);
        MainFrame mainFrame = new MainFrame(registration, catalog, windows::openSettings);
        return new ApplicationComposition(registerProduct, browseProducts, currencySettings, mainFrame, windows);
    }

    RegisterProduct registerProduct() { return registerProduct; }
    BrowseProducts browseProducts() { return browseProducts; }
    ManageCurrency currencySettings() { return currencySettings; }
    MainFrame mainFrame() { return mainFrame; }
    void openEditor(ProductView product) { windows.openEditor(product); }
    void openDeletion(ProductView product) { windows.openDeletion(product); }
    void openSettings() { windows.openSettings(); }

    private static final class ProductWindows {
        private final WindowHost host;
        private final UpdateProduct updateProduct;
        private final DeleteProduct deleteProduct;
        private final ManageCurrency currencySettings;
        private final CatalogRefreshCoordinator refresh;

        private ProductWindows(WindowHost host, UpdateProduct updateProduct, DeleteProduct deleteProduct,
                               ManageCurrency currencySettings, CatalogRefreshCoordinator refresh) {
            this.host = Objects.requireNonNull(host);
            this.updateProduct = updateProduct;
            this.deleteProduct = deleteProduct;
            this.currencySettings = currencySettings;
            this.refresh = refresh;
        }

        private void openEditor(ProductView product) {
            host.show("Edit Product", close -> new ProductEditorDialog(updateProduct, refresh, product,
                    currencySettings.load(), ProductEditorDialog::confirm, close));
        }

        private void openDeletion(ProductView product) {
            host.show("Delete Product", close -> new ProductDeletionDialog(deleteProduct, refresh, product,
                    ProductDeletionDialog::confirm, close));
        }

        private void openSettings() {
            host.show("Settings", close -> new SettingsDialog(currencySettings, refresh, () -> { }, () -> { }, close));
        }
    }
}
