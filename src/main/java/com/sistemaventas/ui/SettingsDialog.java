package com.sistemaventas.ui;

import com.sistemaventas.settings.application.ManageCurrency;
import java.awt.GridLayout;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingWorker;

/** Headless-safe currency settings controller; a composition layer may place it in a JDialog. */
public final class SettingsDialog extends JPanel {
    private final ManageCurrency manageCurrency; private final CatalogRefreshCoordinator refresh; private final Runnable loaded, completion, dismiss;
    private final JTextField currency = new JTextField(10); private final JLabel current = new JLabel("Loading currency..."), feedback = new JLabel(" "); private final JButton save = new JButton("Save currency"), cancel = new JButton("Cancel"); private boolean busy;
    public SettingsDialog(ManageCurrency manageCurrency, CatalogRefreshCoordinator refresh) { this(manageCurrency, refresh, () -> { }, () -> { }, () -> { }); }
    SettingsDialog(ManageCurrency manageCurrency, CatalogRefreshCoordinator refresh, Runnable loaded, Runnable completion) { this(manageCurrency, refresh, loaded, completion, () -> { }); }
    public SettingsDialog(ManageCurrency manageCurrency, CatalogRefreshCoordinator refresh, Runnable loaded, Runnable completion, Runnable dismiss) {
        this.manageCurrency = Objects.requireNonNull(manageCurrency); this.refresh = Objects.requireNonNull(refresh); this.loaded = Objects.requireNonNull(loaded); this.completion = Objects.requireNonNull(completion); this.dismiss = Objects.requireNonNull(dismiss); setLayout(new GridLayout(0, 1, 6, 6)); setBackground(VintageTheme.BACKGROUND);
        currency.getAccessibleContext().setAccessibleName("ISO-4217 currency code"); JLabel currencyLabel = new JLabel("ISO-4217 currency code"); currencyLabel.setLabelFor(currency); for (var component : new javax.swing.JComponent[] {current, currencyLabel, currency, save, cancel, feedback}) { VintageTheme.applyTo(component); add(component); } save.addActionListener(event -> save()); cancel.addActionListener(event -> dismiss.run()); load();
    }
    JTextField currencyInput() { return currency; } JLabel currentCurrency() { return current; } JLabel feedback() { return feedback; } JButton saveButton() { return save; } JButton cancelButton() { return cancel; }
    private void load() { setBusy(true); new SwingWorker<java.util.Optional<java.util.Currency>, Void>() { protected java.util.Optional<java.util.Currency> doInBackground() { return manageCurrency.load(); } protected void done() { try { current.setText(get().map(java.util.Currency::getCurrencyCode).orElse("No currency configured.")); } catch (Exception exception) { current.setText("Currency could not be loaded."); } finally { setBusy(false); loaded.run(); } } }.execute(); }
    private void save() { if (!busy) { setBusy(true); new SwingWorker<ManageCurrency.Result, Void>() { protected ManageCurrency.Result doInBackground() { return manageCurrency.save(currency.getText()); } protected void done() { try { ManageCurrency.Result result = get(); if (result instanceof ManageCurrency.Result.Saved saved) { current.setText(saved.currency().getCurrencyCode()); feedback.setText("Currency saved."); refresh.publish(); dismiss.run(); } else feedback.setText(((ManageCurrency.Result.ValidationFailed) result).message()); } catch (Exception exception) { feedback.setText("Currency could not be saved."); } finally { setBusy(false); completion.run(); } } }.execute(); } }
    private void setBusy(boolean value) { busy = value; currency.setEnabled(!value); save.setEnabled(!value); cancel.setEnabled(!value); }
}
