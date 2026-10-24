package com.sistemaventas.ui;

import com.sistemaventas.inventory.application.BrowseProducts;
import com.sistemaventas.inventory.application.ProductView;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

/** Headless-safe catalog view; all catalog queries execute outside the EDT. */
public final class ProductCatalogPanel extends JPanel {
    private static final String[] COLUMNS = {"ID", "Name", "Category", "Price", "Stock"};
    private final BrowseProducts browseProducts;
    private final Supplier<Optional<java.util.Currency>> currency;
    private final Consumer<ProductView> editAction;
    private final Consumer<ProductView> deleteAction;
    private final Runnable completionHook;
    private final JTextField filterInput = field("Product name filter");
    private final JButton filterButton = new JButton("Search");
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton editButton = new JButton("Edit selected product");
    private final JButton deleteButton = new JButton("Delete selected product");
    private final JLabel status = new JLabel("Loading products...");
    private final DefaultTableModel model = new DefaultTableModel(COLUMNS, 0) { @Override public boolean isCellEditable(int row, int column) { return false; } };
    private final JTable table = new JTable(model);
    private final AtomicLong generation = new AtomicLong();
    private List<ProductView> displayed = List.of();

    public ProductCatalogPanel(BrowseProducts browseProducts, CatalogRefreshCoordinator refreshCoordinator,
                               Supplier<Optional<java.util.Currency>> currency, Consumer<ProductView> editAction,
                               Consumer<ProductView> deleteAction) {
        this(browseProducts, refreshCoordinator, currency, editAction, deleteAction, () -> { });
    }

    ProductCatalogPanel(BrowseProducts browseProducts, CatalogRefreshCoordinator refreshCoordinator,
                        Supplier<Optional<java.util.Currency>> currency, Consumer<ProductView> editAction,
                        Consumer<ProductView> deleteAction, Runnable completionHook) {
        this.browseProducts = Objects.requireNonNull(browseProducts); this.currency = Objects.requireNonNull(currency);
        this.editAction = Objects.requireNonNull(editAction); this.deleteAction = Objects.requireNonNull(deleteAction);
        this.completionHook = Objects.requireNonNull(completionHook);
        setLayout(new BorderLayout(8, 8)); setBackground(VintageTheme.BACKGROUND);
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING)); controls.setBackground(VintageTheme.BACKGROUND);
        for (var control : List.of(filterInput, filterButton, refreshButton, editButton, deleteButton)) { VintageTheme.applyTo(control); controls.add(control); }
        filterButton.addActionListener(event -> refresh()); refreshButton.addActionListener(event -> refresh());
        editButton.addActionListener(event -> selected(editAction)); deleteButton.addActionListener(event -> selected(deleteAction));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); table.getSelectionModel().addListSelectionListener(event -> updateSelection());
        VintageTheme.applyTo(table); VintageTheme.applyTo(status); status.getAccessibleContext().setAccessibleName("Catalog status");
        add(controls, BorderLayout.NORTH); add(new JScrollPane(table), BorderLayout.CENTER); add(status, BorderLayout.SOUTH);
        refreshCoordinator.subscribe(this::refresh); refresh();
    }

    String[] columns() { return COLUMNS.clone(); }
    JTextField filterInput() { return filterInput; } JButton refreshButton() { return refreshButton; }
    JButton editButton() { return editButton; } JButton deleteButton() { return deleteButton; } JLabel status() { return status; }
    List<String> ids() { return displayed.stream().map(product -> Long.toString(product.id())).toList(); }
    String priceAt(int row) { return (String) model.getValueAt(row, 3); }
    void selectRow(int row) { table.setRowSelectionInterval(row, row); }
    void applyFilter() { refresh(); }

    void refresh() {
        long request = generation.incrementAndGet(); String query = filterInput.getText(); setBusy(true);
        new SwingWorker<List<ProductView>, Void>() {
            @Override protected List<ProductView> doInBackground() { return browseProducts.search(query); }
            @Override protected void done() {
                try { if (generation.get() == request) show(get()); }
                catch (Exception exception) { if (generation.get() == request) status.setText("Products could not be loaded. Please try again."); }
                finally { if (generation.get() == request) setBusy(false); completionHook.run(); }
            }
        }.execute();
    }

    private void show(List<ProductView> products) {
        displayed = List.copyOf(products); model.setRowCount(0); PricePresenter presenter = new PricePresenter(currency.get());
        for (ProductView product : displayed) model.addRow(new Object[] {product.id(), product.name(), product.categoryName(), presenter.format(product.price()), product.stock()});
        status.setText(displayed.isEmpty() ? (filterInput.getText().trim().isEmpty() ? "No products have been added yet." : "No products match your search.") : " ");
        updateSelection();
    }
    private void selected(Consumer<ProductView> action) { int row = table.getSelectedRow(); if (row >= 0) action.accept(displayed.get(row)); }
    private void updateSelection() { boolean selected = table.getSelectedRow() >= 0 && !isBusy(); editButton.setEnabled(selected); deleteButton.setEnabled(selected); }
    private boolean isBusy() { return !filterInput.isEnabled(); }
    private void setBusy(boolean busy) { filterInput.setEnabled(!busy); filterButton.setEnabled(!busy); refreshButton.setEnabled(!busy); if (busy) { editButton.setEnabled(false); deleteButton.setEnabled(false); } else updateSelection(); }
    private static JTextField field(String name) { JTextField field = new JTextField(16); field.setName(name); field.getAccessibleContext().setAccessibleName(name); return field; }
}
