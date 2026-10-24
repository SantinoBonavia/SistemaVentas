package com.sistemaventas.ui;

import com.sistemaventas.inventory.application.DuplicateConfirmation;
import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.RegisterProduct;
import com.sistemaventas.inventory.application.RegistrationResult;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** Product registration form that keeps all application work off the EDT. */
public final class ProductRegistrationPanel extends JPanel {
    @FunctionalInterface
    public interface DuplicateConfirmationBoundary {
        boolean confirmDuplicate(DuplicateConfirmation confirmation);
    }

    private final RegisterProduct registerProduct;
    private final CatalogRefreshCoordinator refreshCoordinator;
    private final DuplicateConfirmationBoundary duplicateConfirmation;
    private final Runnable completionHook;
    private final JTextField nameInput = field("Product name");
    private final JTextField categoryInput = field("Category");
    private final JTextField priceInput = field("Price");
    private final JTextField stockInput = field("Stock");
    private final JButton submitButton = new JButton("Create Product");
    private final JLabel feedback = new JLabel(" ");
    private final JLabel nameFeedback = new JLabel(" ");
    private boolean busy;

    public ProductRegistrationPanel(RegisterProduct registerProduct, CatalogRefreshCoordinator refreshCoordinator) {
        this(registerProduct, refreshCoordinator, ProductRegistrationPanel::confirmWithDialog, () -> { });
    }

    ProductRegistrationPanel(RegisterProduct registerProduct, CatalogRefreshCoordinator refreshCoordinator,
                             DuplicateConfirmationBoundary duplicateConfirmation, Runnable completionHook) {
        this.registerProduct = Objects.requireNonNull(registerProduct);
        this.refreshCoordinator = Objects.requireNonNull(refreshCoordinator);
        this.duplicateConfirmation = Objects.requireNonNull(duplicateConfirmation);
        this.completionHook = Objects.requireNonNull(completionHook);
        setLayout(new GridBagLayout());
        setBackground(VintageTheme.BACKGROUND);
        addRow("Name", nameInput, 0);
        addRow("Category", categoryInput, 1);
        addRow("Price", priceInput, 2);
        addRow("Stock", stockInput, 3);
        submitButton.setName("Create Product");
        VintageTheme.applyTo(submitButton);
        submitButton.addActionListener(event -> submit());
        add(submitButton, constraints(0, 4, 2));
        feedback.setName("Registration feedback");
        feedback.getAccessibleContext().setAccessibleName("Registration feedback");
        nameFeedback.setName("Name validation feedback");
        nameFeedback.getAccessibleContext().setAccessibleName("Name validation feedback");
        add(feedback, constraints(0, 5, 2));
        add(nameFeedback, constraints(0, 6, 2));
    }

    JTextField nameInput() { return nameInput; }
    JTextField categoryInput() { return categoryInput; }
    JTextField priceInput() { return priceInput; }
    JTextField stockInput() { return stockInput; }
    JButton submitButton() { return submitButton; }
    JLabel feedback() { return feedback; }
    JLabel nameFeedback() { return nameFeedback; }

    private void submit() {
        if (busy) return;
        nameFeedback.setText(" ");
        feedback.setText(" ");
        setBusy(true);
        ProductRegistrationCommand command = new ProductRegistrationCommand(
                nameInput.getText(), priceInput.getText(), categoryInput.getText(), stockInput.getText()
        );
        runWorker(() -> registerProduct.register(command), this::handleResult);
    }

    private void handleResult(RegistrationResult result) {
        if (result instanceof RegistrationResult.Created created) {
            feedback.setText(created.message());
            nameInput.setText("");
            priceInput.setText("");
            stockInput.setText("");
            refreshCoordinator.publish();
            finish();
        } else if (result instanceof RegistrationResult.ValidationFailed validation) {
            feedback.setText(validation.message());
            JTextField invalidInput = inputFor(validation.field());
            invalidInput.setToolTipText(validation.message());
            invalidInput.getAccessibleContext().setAccessibleDescription(validation.message());
            if ("name".equals(validation.field())) nameFeedback.setText(validation.message());
            finish();
        } else if (result instanceof RegistrationResult.ConfirmationRequired confirmation) {
            resolveDuplicate(confirmation.confirmation());
        } else if (result instanceof RegistrationResult.Failure failure) {
            feedback.setText(failure.message());
            finish();
        }
    }

    private void resolveDuplicate(DuplicateConfirmation confirmation) {
        if (duplicateConfirmation.confirmDuplicate(confirmation)) {
            runWorker(() -> registerProduct.confirm(confirmation.token()), this::handleResult);
        } else {
            runWorker(() -> {
                registerProduct.cancel(confirmation);
                return null;
            }, ignored -> finish());
        }
    }

    private void runWorker(BackgroundWork work, java.util.function.Consumer<RegistrationResult> success) {
        new SwingWorker<RegistrationResult, Void>() {
            @Override
            protected RegistrationResult doInBackground() {
                return work.run();
            }

            @Override
            protected void done() {
                try {
                    success.accept(get());
                } catch (Exception exception) {
                    feedback.setText("Product could not be registered. Please try again.");
                    finish();
                }
            }
        }.execute();
    }

    private void finish() {
        setBusy(false);
        completionHook.run();
    }

    private void setBusy(boolean value) {
        busy = value;
        nameInput.setEnabled(!value);
        categoryInput.setEnabled(!value);
        priceInput.setEnabled(!value);
        stockInput.setEnabled(!value);
        submitButton.setEnabled(!value);
    }

    private void addRow(String label, JComponent component, int row) {
        JLabel fieldLabel = new JLabel(label);
        fieldLabel.setLabelFor(component);
        VintageTheme.applyTo(component);
        add(fieldLabel, constraints(0, row, 1));
        add(component, constraints(1, row, 1));
    }

    private JTextField inputFor(String field) {
        return switch (field) {
            case "name" -> nameInput;
            case "category" -> categoryInput;
            case "price" -> priceInput;
            case "stock" -> stockInput;
            default -> nameInput;
        };
    }

    private static JTextField field(String accessibleName) {
        JTextField field = new JTextField(16);
        field.setName(accessibleName);
        field.getAccessibleContext().setAccessibleName(accessibleName);
        return field;
    }

    private static GridBagConstraints constraints(int x, int y, int width) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.gridwidth = width;
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        return constraints;
    }

    private static boolean confirmWithDialog(DuplicateConfirmation ignored) {
        return JOptionPane.showConfirmDialog(null, "A product with this name already exists. Create it anyway?",
                "Confirm duplicate product", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    @FunctionalInterface
    private interface BackgroundWork {
        RegistrationResult run();
    }
}
