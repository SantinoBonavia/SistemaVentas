package com.sistemaventas.ui;

import com.sistemaventas.inventory.application.DeleteProduct;
import com.sistemaventas.inventory.application.DeleteResult;
import com.sistemaventas.inventory.application.ProductDeletionConfirmation;
import com.sistemaventas.inventory.application.ProductView;
import java.awt.GridLayout;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/** Headless-safe deletion controller; a composition layer may place it in a JDialog. */
public final class ProductDeletionDialog extends JPanel {
    public enum Decision { YES, NO, CLOSED }
    @FunctionalInterface public interface DeleteDecisionBoundary { Decision decide(ProductDeletionConfirmation confirmation); }
    private final DeleteProduct deleteProduct; private final CatalogRefreshCoordinator refresh; private final DeleteDecisionBoundary decision; private final Runnable close; private final long id;
    private final JLabel prompt; private final JLabel feedback = new JLabel(" "); private final JButton delete = new JButton("Delete product"), cancel = new JButton("Cancel"); private boolean busy;
    public ProductDeletionDialog(DeleteProduct deleteProduct, CatalogRefreshCoordinator refresh, ProductView product) { this(deleteProduct, refresh, product, ProductDeletionDialog::confirm, () -> { }); }
    public ProductDeletionDialog(DeleteProduct deleteProduct, CatalogRefreshCoordinator refresh, ProductView product, DeleteDecisionBoundary decision, Runnable close) {
        this.deleteProduct = Objects.requireNonNull(deleteProduct); this.refresh = Objects.requireNonNull(refresh); this.decision = Objects.requireNonNull(decision); this.close = Objects.requireNonNull(close); id = product.id(); prompt = new JLabel("Delete product '" + product.name() + "'?"); prompt.getAccessibleContext().setAccessibleName("Deletion prompt");
        setLayout(new GridLayout(0, 1, 6, 6)); setBackground(VintageTheme.BACKGROUND); for (var component : new javax.swing.JComponent[] {prompt, delete, cancel, feedback}) { VintageTheme.applyTo(component); add(component); } delete.addActionListener(event -> start()); cancel.addActionListener(event -> close.run());
    }
    JLabel prompt() { return prompt; } JButton deleteButton() { return delete; } JLabel feedback() { return feedback; }
    private void start() { if (!busy) { setBusy(true); run(() -> deleteProduct.delete(id)); } }
    private void run(java.util.concurrent.Callable<DeleteResult> work) { new SwingWorker<DeleteResult, Void>() { protected DeleteResult doInBackground() throws Exception { return work.call(); } protected void done() { try { handle(get()); } catch (Exception exception) { feedback.setText("Product could not be deleted."); finish(false); } } }.execute(); }
    private void handle(DeleteResult result) { if (result instanceof DeleteResult.Deleted) { feedback.setText("Product deleted successfully."); refresh.publish(); finish(true); } else if (result instanceof DeleteResult.ConfirmationRequired required) resolve(required.confirmation()); else if (result instanceof DeleteResult.Failure failure) { feedback.setText(failure.message()); finish("Deletion cancelled.".equals(failure.message())); } }
    private void resolve(ProductDeletionConfirmation confirmation) { if (decision.decide(confirmation) == Decision.YES) run(() -> deleteProduct.confirm(confirmation.token())); else run(() -> { deleteProduct.cancel(confirmation); return new DeleteResult.Failure("Deletion cancelled."); }); }
    private void finish(boolean shouldClose) { setBusy(false); if (shouldClose) close.run(); } private void setBusy(boolean value) { busy = value; delete.setEnabled(!value); cancel.setEnabled(!value); }
    public static Decision confirm(ProductDeletionConfirmation ignored) { int answer = JOptionPane.showConfirmDialog(null, "Delete this product?", "Confirm deletion", JOptionPane.YES_NO_OPTION); return answer == JOptionPane.YES_OPTION ? Decision.YES : answer == JOptionPane.NO_OPTION ? Decision.NO : Decision.CLOSED; }
}
