package com.sistemaventas.ui;

import com.sistemaventas.store.StoreProfile;
import com.sistemaventas.store.StoreSettings;
import com.sistemaventas.tax.AlicuotaIva;
import com.sistemaventas.tax.CondicionIva;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/** Datos del comercio para los comprobantes e información de IVA. */
public final class SettingsPanel extends JPanel {
    private final Services services;
    private final JTextField name = new JTextField(28);
    private final JTextField legalName = new JTextField(28);
    private final JTextField cuit = new JTextField(16);
    private final JTextField address = new JTextField(28);
    private final JTextField phone = new JTextField(16);
    private final JComboBox<CondicionIva> condicion = new JComboBox<>(new CondicionIva[]{
            CondicionIva.RESPONSABLE_INSCRIPTO, CondicionIva.MONOTRIBUTISTA, CondicionIva.EXENTO});
    private final JTextField pointOfSale = new JTextField(6);
    private final JLabel feedback = Theme.label(" ", Font.PLAIN, 13f, Theme.SUCCESS);

    public SettingsPanel(Services services) {
        super(new BorderLayout());
        this.services = services;
        setBackground(Theme.BACKGROUND);

        JPanel content = new WidthTrackingPanel(new GridBagLayout());
        content.setBackground(Theme.BACKGROUND);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(0, 0, 4, 0);
        content.add(Theme.title("Configuración"), c);
        c.insets = new Insets(0, 0, 16, 0);
        content.add(Theme.muted("Estos datos encabezan cada ticket."), c);

        c.gridwidth = 1;
        c.gridy = 2;
        c.weightx = 0.55;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 14);
        content.add(storeCard(), c);
        c.gridx = 1;
        c.weightx = 0.45;
        c.insets = new Insets(0, 0, 0, 0);
        content.add(ivaCard(), c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 2;
        c.insets = new Insets(14, 0, 0, 0);
        content.add(dataCard(), c);
        c.gridy = 4;
        c.weighty = 1;
        content.add(new JPanel() {{ setOpaque(false); }}, c);

        JScrollPane scroll = new JScrollPane(content);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
        load(services.store().profile());
    }

    JTextField nameInput() { return name; }
    JTextField cuitInput() { return cuit; }
    JTextField pointOfSaleInput() { return pointOfSale; }
    JLabel feedback() { return feedback; }
    void save() { doSave(); }

    private void load(StoreProfile profile) {
        name.setText(profile.name());
        legalName.setText(profile.legalName());
        cuit.setText(profile.cuit());
        address.setText(profile.address());
        phone.setText(profile.phone());
        condicion.setSelectedItem(profile.condicion());
        pointOfSale.setText(Integer.toString(profile.pointOfSale()));
    }

    private JComponent storeCard() {
        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 0, 10, 0);
        card.add(Theme.label("Datos del comercio", Font.BOLD, 15f, Theme.TEXT), c);
        c.gridwidth = 1;
        Theme.placeholder(cuit, "30-12345678-9");
        Theme.placeholder(legalName, "Ej.: El Tornillo S.R.L.");
        String[] labels = {"Nombre de fantasía *", "Razón social", "CUIT", "Domicilio", "Teléfono", "Condición frente al IVA", "Punto de venta"};
        JComponent[] fields = {name, legalName, cuit, address, phone, condicion, pointOfSale};
        for (int i = 0; i < labels.length; i++) {
            c.gridy = i + 1;
            c.gridx = 0;
            c.weightx = 0;
            c.insets = new Insets(4, 0, 4, 12);
            JLabel label = Theme.label(labels[i], Font.PLAIN, 13f, Theme.MUTED);
            label.setLabelFor(fields[i]);
            fields[i].setName(labels[i].replace(" *", ""));
            card.add(label, c);
            c.gridx = 1;
            c.weightx = 1;
            c.insets = new Insets(4, 0, 4, 0);
            card.add(fields[i], c);
        }
        c.gridy = labels.length + 1;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(12, 0, 0, 0);
        JPanel actions = new JPanel(new BorderLayout());
        actions.setOpaque(false);
        actions.add(feedback, BorderLayout.CENTER);
        JButton saveButton = Theme.primary("Guardar cambios");
        saveButton.setName("Guardar configuración");
        saveButton.addActionListener(event -> doSave());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        right.setOpaque(false);
        right.add(saveButton);
        actions.add(right, BorderLayout.EAST);
        card.add(actions, c);
        c.gridy++;
        c.weighty = 1;
        card.add(new JPanel() {{ setOpaque(false); }}, c);
        return card;
    }

    private JComponent ivaCard() {
        JPanel card = Theme.card();
        card.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridwidth = 3;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 4, 0);
        card.add(Theme.label("Alícuotas de IVA vigentes", Font.BOLD, 15f, Theme.TEXT), c);
        c.insets = new Insets(0, 0, 12, 0);
        card.add(Theme.muted("Ley de IVA, art. 28 · verificadas al "
                + AlicuotaIva.VERIFICADO_AL.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))), c);
        String[] uses = {
                "Herramientas, ferretería, pinturería, electricidad: la mayoría de los artículos.",
                "Algunos bienes de capital y obras de vivienda.",
                "Gas, luz, agua y telecomunicaciones a responsables inscriptos.",
                "Bienes y operaciones exentos."};
        c.gridwidth = 1;
        int row = 2;
        for (AlicuotaIva rate : AlicuotaIva.values()) {
            c.gridy = row++;
            c.gridx = 0;
            c.weightx = 0;
            c.insets = new Insets(6, 0, 6, 12);
            JLabel percent = Theme.label(rate.label(), Font.BOLD, 20f, rate == AlicuotaIva.GENERAL ? Theme.ACCENT_DARK : Theme.TEXT);
            percent.setPreferredSize(new Dimension(70, 28));
            card.add(percent, c);
            c.gridx = 1;
            c.weightx = 1;
            JPanel text = new JPanel(new GridLayout(2, 1));
            text.setOpaque(false);
            text.add(Theme.label(rate.description(), Font.BOLD, 13f, Theme.TEXT));
            text.add(Theme.muted(uses[rate.ordinal()]));
            card.add(text, c);
        }
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(10, 0, 0, 0);
        JLabel note = Theme.muted("<html><div style='width:360px'>Cada producto guarda su alícuota. Si la ley cambia, actualizala desde "
                + "<b>Productos → Editar</b>; los tickets ya emitidos conservan el IVA con que se vendieron.</div></html>");
        card.add(note, c);
        c.gridy++;
        c.weighty = 1;
        card.add(new JPanel() {{ setOpaque(false); }}, c);
        return card;
    }

    private JComponent dataCard() {
        JPanel card = Theme.card();
        card.setLayout(new GridLayout(0, 1, 0, 6));
        card.add(Theme.label("Datos guardados", Font.BOLD, 15f, Theme.TEXT));
        card.add(Theme.muted("Productos, ventas y configuración se guardan automáticamente después de cada cambio en:"));
        JTextField path = new JTextField(services.dataFile() == null ? "Solo en memoria (sesión de prueba)" : services.dataFile().toString());
        path.setEditable(false);
        path.setColumns(30);
        path.setFont(Theme.mono(12.5f));
        card.add(path);
        card.add(Theme.muted("Para hacer una copia de seguridad, copiá esa carpeta (incluye las fotos de los productos)."));
        return card;
    }

    private void doSave() {
        StoreSettings.Result result = services.store().save(name.getText(), legalName.getText(), cuit.getText(),
                address.getText(), phone.getText(), (CondicionIva) condicion.getSelectedItem(), pointOfSale.getText());
        if (result instanceof StoreSettings.Result.Saved saved) {
            load(saved.profile());
            feedback.setForeground(Theme.SUCCESS);
            feedback.setText("✔ Cambios guardados.");
            services.events().publish();
        } else {
            feedback.setForeground(Theme.DANGER);
            feedback.setText(((StoreSettings.Result.Invalid) result).message());
        }
    }

    /** Panel que ocupa el ancho visible del scroll en lugar de desbordarlo. */
    private static final class WidthTrackingPanel extends JPanel implements javax.swing.Scrollable {
        WidthTrackingPanel(java.awt.LayoutManager layout) {
            super(layout);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(java.awt.Rectangle visible, int orientation, int direction) {
            return visible.height - 40;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof javax.swing.JViewport viewport && viewport.getHeight() > getPreferredSize().height;
        }
    }
}
