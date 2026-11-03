package com.sistemaventas.ui;

import com.sistemaventas.inventory.application.BrowseProducts;
import com.sistemaventas.inventory.application.DeleteProduct;
import com.sistemaventas.inventory.application.ReceiveStock;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.inventory.application.UpdateProduct;
import com.sistemaventas.sales.application.CancelSale;
import com.sistemaventas.sales.application.RegisterSale;
import com.sistemaventas.sales.application.SalesReport;
import com.sistemaventas.store.StoreSettings;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.function.Supplier;

/** Casos de uso y recursos compartidos por las pantallas. */
public record Services(BrowseProducts browse, RegisterProduct register, UpdateProduct update, DeleteProduct delete,
                       ReceiveStock receiveStock, RegisterSale registerSale, CancelSale cancelSale,
                       SalesReport report, StoreSettings store, Supplier<List<String>> categories,
                       Path photosDirectory, Path dataFile, CatalogRefreshCoordinator events, Dialogs dialogs,
                       Clock clock) {
}
