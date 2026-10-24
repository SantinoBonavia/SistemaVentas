package com.sistemaventas.ui;

import com.sistemaventas.inventory.application.ProductUpdateConfirmation;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.application.UpdateProduct;
import com.sistemaventas.inventory.application.UpdateProductCommand;
import com.sistemaventas.inventory.application.UpdateResult;
import java.awt.GridLayout;
import java.util.Currency;
import java.util.Objects;
import java.util.Optional;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** Headless-safe edit controller; a composition layer may place it in a JDialog. */
public final class ProductEditorDialog extends JPanel {
    public enum Decision { YES, NO, CLOSED }
    @FunctionalInterface public interface UpdateDecisionBoundary { Decision decide(ProductUpdateConfirmation confirmation); }
    private final UpdateProduct updateProduct; private final CatalogRefreshCoordinator refresh; private final UpdateDecisionBoundary decision; private final Runnable close;
    private final JTextField name = field("Product name"), category = field("Category"), price = field("Price"), stock = field("Stock");
    private final JButton save = new JButton("Save changes"), cancel = new JButton("Cancel"); private final JLabel feedback = new JLabel(" "); private boolean busy;
    public ProductEditorDialog(UpdateProduct updateProduct, CatalogRefreshCoordinator refresh, ProductView product) { this(updateProduct, refresh, product, ProductEditorDialog::confirm, () -> { }); }
    public ProductEditorDialog(UpdateProduct updateProduct, CatalogRefreshCoordinator refresh, ProductView product, UpdateDecisionBoundary decision, Runnable close) {
        this(updateProduct, refresh, product, Optional.of(Currency.getInstance("ARS")), decision, close);
    }
    public ProductEditorDialog(UpdateProduct updateProduct, CatalogRefreshCoordinator refresh, ProductView product,
                               Optional<Currency> currency, UpdateDecisionBoundary decision, Runnable close) {
        this.updateProduct = Objects.requireNonNull(updateProduct); this.refresh = Objects.requireNonNull(refresh); this.decision = Objects.requireNonNull(decision); this.close = Objects.requireNonNull(close);
        setLayout(new GridLayout(0, 2, 6, 6)); setBackground(VintageTheme.BACKGROUND); name.setText(product.name()); category.setText(product.categoryName()); price.setText(new PricePresenter(currency).format(product.price())); stock.setText(Integer.toString(product.stock()));
        addField("Product name", name); addField("Category", category); addField("Price", price); addField("Stock", stock);
        for (var component : new javax.swing.JComponent[] {save, cancel, feedback}) { VintageTheme.applyTo(component); add(component); }
        save.addActionListener(event -> submit(product.id())); cancel.addActionListener(event -> close.run());
    }
    JTextField nameInput() { return name; } JTextField categoryInput() { return category; } JTextField priceInput() { return price; } JTextField stockInput() { return stock; } JButton saveButton() { return save; } JLabel feedback() { return feedback; }
    private void submit(long id) { if (!busy) { setBusy(true); run(() -> updateProduct.update(new UpdateProductCommand(id, name.getText(), price.getText(), category.getText(), stock.getText()))); } }
    private void run(java.util.concurrent.Callable<UpdateResult> work) { new SwingWorker<UpdateResult, Void>() { protected UpdateResult doInBackground() throws Exception { return work.call(); } protected void done() { try { handle(get()); } catch (Exception exception) { feedback.setText("Product could not be updated."); finish(false); } } }.execute(); }
    private void handle(UpdateResult result) { if (result instanceof UpdateResult.Updated) { feedback.setText("Product updated successfully."); refresh.publish(); finish(true); } else if (result instanceof UpdateResult.ConfirmationRequired required) resolve(required.confirmation()); else if (result instanceof UpdateResult.ValidationFailed invalid) { feedback.setText(invalid.message()); input(invalid.field()).setToolTipText(invalid.message()); finish(false); } else if (result instanceof UpdateResult.Failure failure) { feedback.setText(failure.message()); finish("Update cancelled.".equals(failure.message())); } }
    private void resolve(ProductUpdateConfirmation confirmation) { if (decision.decide(confirmation) == Decision.YES) run(() -> updateProduct.confirm(confirmation.token())); else run(() -> { updateProduct.cancel(confirmation); return new UpdateResult.Failure("Update cancelled."); }); }
    private void finish(boolean shouldClose) { setBusy(false); if (shouldClose) close.run(); } private void setBusy(boolean value) { busy = value; for (var component : new javax.swing.JComponent[] {name, category, price, stock, save, cancel}) component.setEnabled(!value); }
    private JTextField input(String field) { return switch (field) { case "category" -> category; case "price" -> price; case "stock" -> stock; default -> name; }; }
    private void addField(String labelText, JTextField input) { JLabel label = new JLabel(labelText); label.setLabelFor(input); VintageTheme.applyTo(label); VintageTheme.applyTo(input); add(label); add(input); }
    private static JTextField field(String label) { JTextField input = new JTextField(14); input.getAccessibleContext().setAccessibleName(label); return input; }
    public static Decision confirm(ProductUpdateConfirmation ignored) { int answer = JOptionPane.showConfirmDialog(null, "A product with this name already exists. Save anyway?", "Confirm duplicate product", JOptionPane.YES_NO_OPTION); return answer == JOptionPane.YES_OPTION ? Decision.YES : answer == JOptionPane.NO_OPTION ? Decision.NO : Decision.CLOSED; }
}
