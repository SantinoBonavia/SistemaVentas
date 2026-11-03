package com.sistemaventas.ui;

import com.sistemaventas.inventory.application.ProductInputValidator;
import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.application.RegistrationResult;
import com.sistemaventas.inventory.application.UpdateProductCommand;
import com.sistemaventas.inventory.application.UpdateResult;
import com.sistemaventas.sales.application.Pesos;
import com.sistemaventas.tax.AlicuotaIva;
import com.sistemaventas.tax.IvaCalculator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/** Alta y edición de producto con cálculo de IVA en vivo y foto. */
final class ProductFormPanel extends JPanel {
    private final Services services;
    private final ProductView editing;
    private final Runnable close;
    private final JTextField name = new JTextField(24);
    private final JTextField code = new JTextField(10);
    private final JComboBox<String> category = new JComboBox<>();
    private final JComboBox<AlicuotaIva> iva = new JComboBox<>(AlicuotaIva.values());
    private final JTextField net = new JTextField(10);
    private final JLabel ivaAmount = Theme.label("$0,00", Font.PLAIN, 14f, Theme.TEXT);
    private final JTextField finalPrice = new JTextField(10);
    private final JTextField stock = new JTextField(6);
    private final JTextField minStock = new JTextField(6);
    private final JLabel preview = new JLabel();
    private final JComboBox<String> art = new JComboBox<>(ToolArt.keys().toArray(String[]::new));
    private final JLabel feedback = Theme.label(" ", Font.PLAIN, 12.5f, Theme.DANGER);
    private final JButton save = Theme.primary("Guardar producto");
    private String image;
    private boolean syncing;

    ProductFormPanel(Services services, ProductView editing, Runnable close) {
        super(new BorderLayout(0, 14));
        this.services = services;
        this.editing = editing;
        this.close = close;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));

        JPanel header = new JPanel(new GridLayout(2, 1));
        header.setOpaque(false);
        header.add(Theme.label(editing == null ? "Nuevo producto" : "Editar producto", Font.BOLD, 20f, Theme.TEXT));
        header.add(Theme.muted("El precio final es el que paga el cliente en el mostrador (IVA incluido)."));
        add(header, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);
        body.add(buildFields(), BorderLayout.CENTER);
        body.add(buildPhoto(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
        add(buildActions(), BorderLayout.SOUTH);

        services.categories().get().forEach(category::addItem);
        category.setEditable(true);
        if (editing != null) {
            name.setText(editing.name());
            code.setText(editing.code());
            category.setSelectedItem(editing.categoryName());
            iva.setSelectedItem(AlicuotaIva.fromPercent(editing.ivaPercent()).orElse(AlicuotaIva.GENERAL));
            finalPrice.setText(Pesos.plain(editing.price()));
            stock.setText(Integer.toString(editing.stock()));
            minStock.setText(Integer.toString(editing.minStock()));
            image = editing.image();
            syncFromFinal();
        } else {
            category.setSelectedItem("");
            iva.setSelectedItem(AlicuotaIva.GENERAL);
            stock.setText("0");
            minStock.setText("5");
            image = ProductImages.ART_PREFIX + "generico";
        }
        if (image.startsWith(ProductImages.ART_PREFIX)) art.setSelectedItem(image.substring(ProductImages.ART_PREFIX.length()));
        updatePreview();
    }

    JTextField nameInput() { return name; }
    JTextField codeInput() { return code; }
    JComboBox<String> categoryInput() { return category; }
    JComboBox<AlicuotaIva> ivaInput() { return iva; }
    JTextField netInput() { return net; }
    JTextField finalPriceInput() { return finalPrice; }
    JLabel ivaAmountLabel() { return ivaAmount; }
    JTextField stockInput() { return stock; }
    JTextField minStockInput() { return minStock; }
    JButton saveButton() { return save; }
    JLabel feedback() { return feedback; }

    private JComponent buildFields() {
        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 0, 5, 10);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        name.setName("Nombre");
        code.setName("Código");
        category.setName("Categoría");
        iva.setName("Alícuota de IVA");
        net.setName("Precio sin IVA");
        finalPrice.setName("Precio final");
        stock.setName("Stock");
        minStock.setName("Stock mínimo");
        Theme.placeholder(name, "Ej.: Martillo carpintero 27 mm");
        Theme.placeholder(code, "Ej.: MAR-001");
        Theme.placeholder(net, "0,00");
        Theme.placeholder(finalPrice, "0,00");

        int row = 0;
        addField(card, c, row++, "Nombre *", name, 3);
        addField(card, c, row, "Código / SKU", code, 1);
        addFieldAt(card, c, row++, 2, "Categoría *", category);

        JPanel pricing = new JPanel(new GridBagLayout());
        pricing.setBackground(Theme.ACCENT_SOFT);
        pricing.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(new Color(0xFFD8A8), 12),
                BorderFactory.createEmptyBorder(10, 12, 12, 12)));
        GridBagConstraints p = new GridBagConstraints();
        p.insets = new Insets(2, 4, 2, 4);
        p.fill = GridBagConstraints.HORIZONTAL;
        p.weightx = 1;
        p.gridy = 0;
        p.gridx = 0;
        p.gridwidth = 5;
        pricing.add(Theme.label("Cálculo de precio e IVA", Font.BOLD, 13f, Theme.ACCENT_DARK), p);
        p.gridwidth = 1;
        p.gridy = 1;
        String[] headers = {"Precio sin IVA", "", "IVA", "", "Precio final *"};
        for (int i = 0; i < headers.length; i++) {
            p.gridx = i;
            pricing.add(Theme.muted(headers[i]), p);
        }
        p.gridy = 2;
        p.gridx = 0;
        pricing.add(net, p);
        p.gridx = 1;
        p.weightx = 0;
        pricing.add(Theme.label("+", Font.BOLD, 16f, Theme.MUTED), p);
        p.gridx = 2;
        p.weightx = 1;
        JPanel ivaBox = new JPanel(new BorderLayout(6, 0));
        ivaBox.setOpaque(false);
        ivaBox.add(iva, BorderLayout.WEST);
        ivaBox.add(ivaAmount, BorderLayout.CENTER);
        pricing.add(ivaBox, p);
        p.gridx = 3;
        p.weightx = 0;
        pricing.add(Theme.label("=", Font.BOLD, 16f, Theme.MUTED), p);
        p.gridx = 4;
        p.weightx = 1;
        finalPrice.setFont(Theme.font(Font.BOLD, 15f));
        pricing.add(finalPrice, p);
        iva.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                AlicuotaIva rate = (AlicuotaIva) value;
                String text = rate == null ? "" : index < 0 ? rate.label() : rate.label() + "  " + rate.description();
                return super.getListCellRendererComponent(list, text, index, selected, focus);
            }
        });

        c.gridx = 0;
        c.gridy = row++;
        c.gridwidth = 4;
        c.insets = new Insets(10, 0, 10, 0);
        card.add(pricing, c);
        c.insets = new Insets(5, 0, 5, 10);
        c.gridwidth = 1;
        addField(card, c, row, "Stock actual", stock, 1);
        addFieldAt(card, c, row++, 2, "Stock mínimo (alerta)", minStock);
        if (editing != null) {
            c.gridx = 0;
            c.gridy = row++;
            c.gridwidth = 4;
            card.add(Theme.muted("Para ingresos de mercadería usá \"Ingresar stock\" y queda registrado el movimiento."), c);
        }

        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 4;
        c.weighty = 1;
        card.add(new JPanel() {{ setOpaque(false); }}, c);

        net.getDocument().addDocumentListener(PosPanel.onChange(this::syncFromNet));
        finalPrice.getDocument().addDocumentListener(PosPanel.onChange(this::syncFromFinal));
        iva.addActionListener(event -> syncFromFinalOrNet());
        return card;
    }

    private JComponent buildPhoto() {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 10));
        card.add(Theme.label("Foto", Font.BOLD, 14f, Theme.TEXT), BorderLayout.NORTH);
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        preview.setOpaque(true);
        preview.setBackground(Theme.ACCENT_SOFT);
        preview.setPreferredSize(new Dimension(200, 200));
        preview.getAccessibleContext().setAccessibleName("Vista previa de la foto");
        card.add(preview, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 6));
        buttons.setOpaque(false);
        JButton choose = Theme.secondary("Elegir foto del equipo…");
        choose.setName("Elegir foto");
        choose.addActionListener(event -> choosePhoto());
        art.setName("Ilustración");
        art.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, ToolArt.CATALOG.get((String) value), index, selected, focus);
                label.setIcon(new ImageIcon(ToolArt.image((String) value, 24)));
                label.setIconTextGap(8);
                return label;
            }
        });
        art.addActionListener(event -> {
            if (art.getSelectedItem() != null && !syncing) {
                image = ProductImages.ART_PREFIX + art.getSelectedItem();
                updatePreview();
            }
        });
        buttons.add(choose);
        buttons.add(Theme.muted("o usá una ilustración:"));
        buttons.add(art);
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    private JComponent buildActions() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.add(feedback, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = Theme.secondary("Cancelar");
        cancel.addActionListener(event -> close.run());
        save.setName("Guardar producto");
        save.addActionListener(event -> submit());
        buttons.add(cancel);
        buttons.add(save);
        bar.add(buttons, BorderLayout.EAST);
        return bar;
    }

    private void choosePhoto() {
        Optional<java.nio.file.Path> selected = services.dialogs().choosePhoto();
        if (selected.isEmpty()) return;
        try {
            image = ProductImages.importPhoto(selected.get(), services.photosDirectory());
            feedback.setText(" ");
            updatePreview();
        } catch (IOException exception) {
            feedback.setText("No se pudo usar esa imagen: " + exception.getMessage());
        }
    }

    private void updatePreview() {
        preview.setIcon(new ImageIcon(ProductImages.image(image, ProductImages.isPhoto(image) ? 200 : 150)));
    }

    // ---- cálculo de precios -------------------------------------------------------------------

    private AlicuotaIva rate() {
        return (AlicuotaIva) iva.getSelectedItem();
    }

    private void syncFromNet() {
        if (syncing) return;
        Optional<BigDecimal> value = ProductInputValidator.parsePositivePrice(net.getText());
        syncing = true;
        try {
            if (value.isPresent()) {
                BigDecimal total = IvaCalculator.finalFromNet(value.get(), rate().percent());
                finalPrice.setText(Pesos.plain(total));
                ivaAmount.setText(Pesos.format(total.subtract(value.get())));
            } else {
                ivaAmount.setText("—");
            }
        } finally {
            syncing = false;
        }
    }

    private void syncFromFinal() {
        if (syncing) return;
        Optional<BigDecimal> value = ProductInputValidator.parsePositivePrice(finalPrice.getText());
        syncing = true;
        try {
            if (value.isPresent()) {
                net.setText(Pesos.plain(IvaCalculator.netFromFinal(value.get(), rate().percent())));
                ivaAmount.setText(Pesos.format(IvaCalculator.ivaContained(value.get(), rate().percent())));
            } else {
                ivaAmount.setText("—");
            }
        } finally {
            syncing = false;
        }
    }

    /** Al cambiar la alícuota se conserva el precio final si existe (es el precio de góndola). */
    private void syncFromFinalOrNet() {
        if (!finalPrice.getText().isBlank()) syncFromFinal(); else syncFromNet();
    }

    // ---- guardar ------------------------------------------------------------------------------

    private void submit() {
        String categoryName = category.getEditor().getItem() == null ? "" : category.getEditor().getItem().toString();
        String ivaPercent = rate().percent().toPlainString();
        if (editing == null) {
            RegistrationResult result = services.register().register(new ProductRegistrationCommand(name.getText(),
                    finalPrice.getText(), categoryName, stock.getText(), code.getText(), ivaPercent, minStock.getText(), image));
            if (result instanceof RegistrationResult.ConfirmationRequired required) {
                result = services.dialogs().confirm("Producto repetido",
                        "Ya existe un producto llamado \"" + name.getText().trim() + "\". ¿Cargarlo igual?", "Cargar igual")
                        ? services.register().confirm(required.confirmation().token())
                        : cancelRegistration(required);
            }
            handle(result instanceof RegistrationResult.Created, messageOf(result));
        } else {
            UpdateResult result = services.update().update(new UpdateProductCommand(editing.id(), name.getText(),
                    finalPrice.getText(), categoryName, stock.getText(), code.getText(), ivaPercent, minStock.getText(), image));
            if (result instanceof UpdateResult.ConfirmationRequired required) {
                if (services.dialogs().confirm("Producto repetido",
                        "Ya existe otro producto con ese nombre. ¿Guardar igual?", "Guardar igual")) {
                    result = services.update().confirm(required.confirmation().token());
                } else {
                    services.update().cancel(required.confirmation());
                    return;
                }
            }
            handle(result instanceof UpdateResult.Updated, messageOf(result));
        }
    }

    private RegistrationResult cancelRegistration(RegistrationResult.ConfirmationRequired required) {
        services.register().cancel(required.confirmation());
        return null;
    }

    private void handle(boolean success, String message) {
        if (success) {
            services.events().publish();
            close.run();
        } else if (message != null) {
            feedback.setText(message);
        }
    }

    private static String messageOf(Object result) {
        return switch (result) {
            case RegistrationResult.ValidationFailed failed -> failed.message();
            case RegistrationResult.Failure failure -> failure.message();
            case UpdateResult.ValidationFailed failed -> failed.message();
            case UpdateResult.Failure failure -> failure.message();
            case null, default -> null;
        };
    }

    private static void addField(JPanel panel, GridBagConstraints c, int row, String label, JComponent field, int width) {
        addFieldAt(panel, c, row, 0, label, field);
        if (width > 1) {
            panel.remove(field);
            GridBagConstraints wide = (GridBagConstraints) c.clone();
            wide.gridx = 1;
            wide.gridy = row;
            wide.gridwidth = width;
            wide.weightx = 1;
            panel.add(field, wide);
        }
    }

    private static void addFieldAt(JPanel panel, GridBagConstraints c, int row, int column, String label, JComponent field) {
        JLabel text = Theme.label(label, Font.PLAIN, 13f, Theme.MUTED);
        text.setLabelFor(field);
        GridBagConstraints l = (GridBagConstraints) c.clone();
        l.gridx = column;
        l.gridy = row;
        l.gridwidth = 1;
        l.weightx = 0;
        panel.add(text, l);
        GridBagConstraints f = (GridBagConstraints) c.clone();
        f.gridx = column + 1;
        f.gridy = row;
        f.gridwidth = 1;
        f.weightx = 1;
        panel.add(field, f);
    }
}
