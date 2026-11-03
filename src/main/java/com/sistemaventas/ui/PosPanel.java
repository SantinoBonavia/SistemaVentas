package com.sistemaventas.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.sistemaventas.inventory.application.ProductInputValidator;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.sales.application.Buyer;
import com.sistemaventas.sales.application.Cart;
import com.sistemaventas.sales.application.PaymentMethod;
import com.sistemaventas.sales.application.Pesos;
import com.sistemaventas.sales.application.RegisterSale;
import com.sistemaventas.sales.application.SaleLine;
import com.sistemaventas.sales.application.SaleTotals;
import com.sistemaventas.tax.AlicuotaIva;
import com.sistemaventas.tax.CondicionIva;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Mostrador: catálogo con fotos a la izquierda y ticket en curso a la derecha. */
public final class PosPanel extends JPanel {
    private static final String ALL = "Todas";
    private final Services services;
    private final Cart cart = new Cart();
    private List<ProductView> catalog = List.of();
    private String category = ALL;

    private final JTextField search = new JTextField();
    private final JPanel chips = new JPanel(new WrapFlowLayout(6, 6));
    private final ButtonGroup chipGroup = new ButtonGroup();
    private final GridPanel grid = new GridPanel();
    private final WrapGridLayout gridLayout = new WrapGridLayout(196, ProductCard.HEIGHT, 14);
    private final JLabel resultCount = Theme.muted(" ");

    private final JLabel cartCount = Theme.muted("Sin artículos");
    private final GridPanel lines = new GridPanel();
    private final JLabel subtotal = amountLabel(false);
    private final JLabel discountAmount = amountLabel(false);
    private final JPanel ivaRows = new JPanel(new GridBagLayout());
    private final JLabel total = Theme.label("$0", Font.BOLD, 30f, Theme.TEXT);
    private final JSpinner discount = new JSpinner(new SpinnerNumberModel(0.0, 0.0, 100.0, 5.0));
    private final JComboBox<CondicionIva> buyerType = new JComboBox<>(new CondicionIva[]{
            CondicionIva.CONSUMIDOR_FINAL, CondicionIva.RESPONSABLE_INSCRIPTO, CondicionIva.MONOTRIBUTISTA, CondicionIva.EXENTO});
    private final JTextField buyerCuit = new JTextField();
    private final JTextField buyerName = new JTextField();
    private final JPanel buyerDetails = new JPanel(new GridLayout(1, 2, 8, 0));
    private final JLabel comprobante = Theme.muted(" ");
    private final Map<PaymentMethod, JToggleButton> payments = new EnumMap<>(PaymentMethod.class);
    private final JTextField paidWith = new JTextField();
    private final JLabel change = Theme.label(" ", Font.BOLD, 14f, Theme.SUCCESS);
    private final JPanel cashRow = new JPanel(new BorderLayout(8, 0));
    private final JLabel feedback = Theme.label(" ", Font.PLAIN, 12.5f, Theme.DANGER);
    private final JButton charge = Theme.success("Cobrar");
    private boolean updating;

    public PosPanel(Services services) {
        this.services = services;
        setLayout(new BorderLayout());
        setBackground(Theme.BACKGROUND);
        add(buildCatalog(), BorderLayout.CENTER);
        add(buildTicket(), BorderLayout.EAST);
        services.events().subscribe(this::reload);
        reload();
    }

    // ---- API usada por la ventana principal y las pruebas --------------------------------------

    public void focusSearch() {
        search.requestFocusInWindow();
        search.selectAll();
    }

    public void checkout() {
        if (charge.isEnabled()) charge.doClick();
    }

    JTextField searchField() { return search; }
    JButton chargeButton() { return charge; }
    JLabel feedback() { return feedback; }
    JLabel totalLabel() { return total; }
    JTextField paidWithField() { return paidWith; }
    JComboBox<CondicionIva> buyerTypeBox() { return buyerType; }
    JTextField buyerCuitField() { return buyerCuit; }
    JTextField buyerNameField() { return buyerName; }
    JLabel changeLabel() { return change; }
    Cart cart() { return cart; }
    void selectPayment(PaymentMethod method) { payments.get(method).doClick(); }

    List<ProductCard> cards() {
        List<ProductCard> cards = new ArrayList<>();
        for (var component : grid.getComponents()) if (component instanceof ProductCard card) cards.add(card);
        return cards;
    }

    void addToCart(ProductView product) {
        Optional<String> problem = cart.add(product);
        feedback.setText(problem.orElse(" "));
        refreshTicket();
        renderGrid();
    }

    // ---- catálogo -----------------------------------------------------------------------------

    private JComponent buildCatalog() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(18, 20, 0, 12));

        search.setName("Buscar producto");
        search.getAccessibleContext().setAccessibleName("Buscar producto");
        search.setFont(Theme.font(Font.PLAIN, 15f));
        search.putClientProperty(FlatClientProperties.STYLE, "margin: 8,10,8,10; arc: 12");
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new com.formdev.flatlaf.icons.FlatSearchIcon());
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        Theme.placeholder(search, "Buscá por nombre o código y presioná Enter para agregar   (F5)");
        search.getDocument().addDocumentListener(onChange(this::renderGrid));
        search.addActionListener(event -> addFromSearch());

        chips.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(search, BorderLayout.NORTH);
        JPanel chipRow = new JPanel(new BorderLayout());
        chipRow.setOpaque(false);
        chipRow.add(chips, BorderLayout.CENTER);
        chipRow.add(resultCount, BorderLayout.EAST);
        top.add(chipRow, BorderLayout.CENTER);

        grid.setLayout(gridLayout);
        grid.setBackground(Theme.BACKGROUND);
        grid.setBorder(BorderFactory.createEmptyBorder(4, 2, 20, 6));
        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(Theme.BACKGROUND);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getViewport().addChangeListener(event -> grid.revalidate());

        panel.add(top, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void reload() {
        catalog = services.browse().list();
        cart.refresh(catalog);
        rebuildChips();
        renderGrid();
        refreshTicket();
    }

    private void rebuildChips() {
        Set<String> categories = new LinkedHashSet<>();
        categories.add(ALL);
        catalog.stream().map(ProductView::categoryName).sorted().forEach(categories::add);
        if (!categories.contains(category)) category = ALL;
        chips.removeAll();
        for (var button : java.util.Collections.list(chipGroup.getElements())) chipGroup.remove(button);
        for (String name : categories) {
            JToggleButton chip = new JToggleButton(name);
            Theme.chip(chip);
            chip.setSelected(name.equals(category));
            chip.addActionListener(event -> {
                category = name;
                renderGrid();
            });
            chipGroup.add(chip);
            chips.add(chip);
        }
        chips.revalidate();
        chips.repaint();
    }

    private List<ProductView> visibleProducts() {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        return catalog.stream()
                .filter(product -> ALL.equals(category) || product.categoryName().equals(category))
                .filter(product -> query.isEmpty()
                        || product.name().toLowerCase(Locale.ROOT).contains(query)
                        || product.code().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private void renderGrid() {
        List<ProductView> visible = visibleProducts();
        grid.removeAll();
        grid.setLayout(visible.isEmpty() ? new BorderLayout() : gridLayout);
        for (ProductView product : visible) {
            grid.add(new ProductCard(product, cart.quantityOf(product.id()), this::addToCart));
        }
        if (visible.isEmpty()) {
            JLabel empty = Theme.label(catalog.isEmpty()
                    ? "Todavía no hay productos. Cargalos desde la sección Productos."
                    : "No hay productos que coincidan con la búsqueda.", Font.PLAIN, 14f, Theme.MUTED);
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            empty.setBorder(BorderFactory.createEmptyBorder(60, 0, 0, 0));
            grid.add(empty, BorderLayout.NORTH);
        }
        resultCount.setText(visible.size() == 1 ? "1 producto" : visible.size() + " productos");
        grid.revalidate();
        grid.repaint();
    }

    private void addFromSearch() {
        String query = search.getText().trim();
        if (query.isEmpty()) return;
        Optional<ProductView> byCode = catalog.stream().filter(product -> product.code().equalsIgnoreCase(query)).findFirst();
        List<ProductView> visible = visibleProducts();
        Optional<ProductView> match = byCode.isPresent() ? byCode : visible.size() == 1 ? Optional.of(visible.getFirst()) : Optional.empty();
        if (match.isPresent()) {
            addToCart(match.get());
            search.setText("");
        } else {
            feedback.setText(visible.isEmpty() ? "No se encontró \"" + query + "\"." : "Hay varios resultados: elegí uno de la lista.");
        }
    }

    // ---- ticket -------------------------------------------------------------------------------

    private JComponent buildTicket() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Theme.SURFACE);
        panel.setPreferredSize(new Dimension(430, 10));
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, Theme.BORDER));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(BorderFactory.createEmptyBorder(18, 20, 10, 16));
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(Theme.label("Ticket actual", Font.BOLD, 18f, Theme.TEXT));
        titles.add(cartCount);
        JButton clear = Theme.link("Vaciar");
        clear.setName("Vaciar ticket");
        clear.addActionListener(event -> {
            cart.clear();
            discount.setValue(0.0);
            feedback.setText(" ");
            refreshTicket();
            renderGrid();
        });
        header.add(titles, BorderLayout.CENTER);
        header.add(clear, BorderLayout.EAST);

        lines.setLayout(new BoxLayout(lines, BoxLayout.Y_AXIS));
        lines.setBackground(Theme.SURFACE);
        JScrollPane scroll = new JScrollPane(lines);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Theme.BORDER));
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.getViewport().setBackground(Theme.SURFACE);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buildCheckout(), BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildCheckout() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 18, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);

        discount.setName("Descuento");
        discount.setEditor(new JSpinner.NumberEditor(discount, "0.#'%'"));
        discount.setPreferredSize(new Dimension(92, 28));
        discount.addChangeListener(event -> {
            if (!updating) refreshTicket();
        });
        panel.add(row(Theme.muted("Subtotal"), subtotal), c);
        JPanel discountLeft = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
        discountLeft.setOpaque(false);
        discountLeft.add(Theme.muted("Descuento  "));
        discountLeft.add(discount);
        panel.add(row(discountLeft, discountAmount), c);
        ivaRows.setOpaque(false);
        panel.add(ivaRows, c);

        JPanel totalRow = row(Theme.label("TOTAL", Font.BOLD, 15f, Theme.TEXT), total);
        totalRow.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER), BorderFactory.createEmptyBorder(6, 0, 4, 0)));
        total.setName("Total");
        c.insets = new Insets(6, 0, 8, 0);
        panel.add(totalRow, c);

        c.insets = new Insets(2, 0, 2, 0);
        buyerType.setName("Condición del cliente");
        buyerType.addActionListener(event -> updateBuyer());
        Theme.placeholder(buyerCuit, "CUIT del cliente");
        Theme.placeholder(buyerName, "Razón social");
        buyerCuit.setName("CUIT del cliente");
        buyerName.setName("Razón social");
        buyerDetails.setOpaque(false);
        buyerDetails.add(buyerCuit);
        buyerDetails.add(buyerName);
        JPanel buyerRow = new JPanel(new BorderLayout(8, 0));
        buyerRow.setOpaque(false);
        buyerRow.add(Theme.muted("Cliente"), BorderLayout.WEST);
        buyerRow.add(buyerType, BorderLayout.CENTER);
        buyerRow.add(comprobante, BorderLayout.EAST);
        panel.add(buyerRow, c);
        panel.add(buyerDetails, c);

        JPanel paymentRow = new JPanel(new GridLayout(1, PaymentMethod.values().length, 6, 0));
        paymentRow.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (PaymentMethod method : PaymentMethod.values()) {
            JToggleButton button = new JToggleButton(method == PaymentMethod.TRANSFERENCIA ? "Transf." : method.label());
            Theme.chip(button);
            button.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 10; background: #FFFFFF; borderColor: #D1D5DB; selectedBackground: #1F2328; selectedForeground: #FFFFFF; focusWidth: 0; margin: 6,2,6,2");
            button.setFont(Theme.font(Font.PLAIN, 12.5f));
            button.setToolTipText(method.label());
            button.setName("Pago " + method.label());
            button.addActionListener(event -> updatePayment());
            group.add(button);
            payments.put(method, button);
            paymentRow.add(button);
        }
        payments.get(PaymentMethod.EFECTIVO).setSelected(true);
        c.insets = new Insets(10, 0, 2, 0);
        panel.add(Theme.muted("Medio de pago"), c);
        c.insets = new Insets(2, 0, 2, 0);
        panel.add(paymentRow, c);

        paidWith.setName("Paga con");
        Theme.placeholder(paidWith, "Paga con $ (opcional)");
        paidWith.getDocument().addDocumentListener(onChange(this::updateChange));
        cashRow.setOpaque(false);
        cashRow.add(paidWith, BorderLayout.CENTER);
        cashRow.add(change, BorderLayout.EAST);
        c.insets = new Insets(6, 0, 2, 0);
        panel.add(cashRow, c);

        panel.add(feedback, c);
        charge.setName("Cobrar");
        charge.setPreferredSize(new Dimension(10, 54));
        charge.addActionListener(event -> doCheckout());
        c.insets = new Insets(6, 0, 0, 0);
        panel.add(charge, c);
        updateBuyer();
        return panel;
    }

    private void refreshTicket() {
        updating = true;
        try {
            BigDecimal percent = BigDecimal.valueOf(((Number) discount.getValue()).doubleValue());
            cart.setDiscountPercent(percent);
        } finally {
            updating = false;
        }
        lines.removeAll();
        if (cart.isEmpty()) {
            lines.add(emptyTicket());
        } else {
            for (SaleLine line : cart.lines()) lines.add(new LineRow(line));
            lines.add(Box.createVerticalGlue());
        }
        lines.revalidate();
        lines.repaint();

        SaleTotals totals = cart.totals();
        int items = cart.itemCount();
        cartCount.setText(items == 0 ? "Sin artículos" : items == 1 ? "1 artículo" : items + " artículos");
        subtotal.setText(Pesos.format(totals.subtotal()));
        discountAmount.setText(totals.discount().signum() == 0 ? "—" : "-" + Pesos.format(totals.discount()));
        ivaRows.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);
        boolean discriminate = "A".equals(letter());
        if (totals.breakdown().isEmpty()) {
            ivaRows.add(row(Theme.muted("IVA contenido 21%"), amount("$0,00")), c);
        }
        for (SaleTotals.IvaBreakdown rate : totals.breakdown()) {
            String label = AlicuotaIva.fromPercent(rate.ivaPercent()).map(AlicuotaIva::label).orElse("");
            if (discriminate) ivaRows.add(row(Theme.muted("Neto gravado " + label), amount(Pesos.format(rate.net()))), c);
            ivaRows.add(row(Theme.muted((discriminate ? "IVA " : "IVA contenido ") + label), amount(Pesos.format(rate.iva()))), c);
        }
        ivaRows.revalidate();
        total.setText(Pesos.format(totals.total()));
        charge.setText(cart.isEmpty() ? "Cobrar   (F12)" : "Cobrar " + Pesos.format(totals.total()) + "   (F12)");
        charge.setEnabled(!cart.isEmpty());
        updateChange();
    }

    private JComponent emptyTicket() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        empty.setAlignmentX(LEFT_ALIGNMENT);
        JPanel content = new JPanel(new GridLayout(0, 1, 0, 4));
        content.setOpaque(false);
        JLabel art = new JLabel(new javax.swing.ImageIcon(ProductImages.image("art:caja", 84)));
        art.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(art);
        JLabel text = Theme.label("El ticket está vacío", Font.BOLD, 14f, Theme.TEXT);
        text.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(text);
        JLabel hint = Theme.muted("Tocá un producto o escaneá su código.");
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        content.add(hint);
        empty.add(content);
        empty.setPreferredSize(new Dimension(380, 220));
        empty.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));
        return empty;
    }

    private void updateBuyer() {
        CondicionIva condicion = (CondicionIva) buyerType.getSelectedItem();
        buyerDetails.setVisible(condicion != CondicionIva.CONSUMIDOR_FINAL);
        comprobante.setText("Comprobante " + letter());
        refreshTicketIfReady();
        revalidate();
    }

    private void refreshTicketIfReady() {
        if (charge.getParent() != null) refreshTicket();
    }

    private String letter() {
        CondicionIva condicion = (CondicionIva) buyerType.getSelectedItem();
        return CondicionIva.comprobanteLetter(services.store().profile().condicion(),
                condicion == null ? CondicionIva.CONSUMIDOR_FINAL : condicion);
    }

    private PaymentMethod selectedPayment() {
        return payments.entrySet().stream().filter(entry -> entry.getValue().isSelected())
                .map(Map.Entry::getKey).findFirst().orElse(PaymentMethod.EFECTIVO);
    }

    private void updatePayment() {
        cashRow.setVisible(selectedPayment() == PaymentMethod.EFECTIVO);
        updateChange();
        revalidate();
    }

    private void updateChange() {
        Optional<BigDecimal> paid = ProductInputValidator.parsePositivePrice(paidWith.getText());
        BigDecimal due = cart.totals().total();
        if (paidWith.getText().isBlank() || cart.isEmpty()) {
            change.setText(" ");
        } else if (paid.isEmpty()) {
            change.setForeground(Theme.DANGER);
            change.setText("Importe inválido");
        } else if (paid.get().compareTo(due) < 0) {
            change.setForeground(Theme.DANGER);
            change.setText("Faltan " + Pesos.format(due.subtract(paid.get())));
        } else {
            change.setForeground(Theme.SUCCESS);
            change.setText("Vuelto " + Pesos.format(paid.get().subtract(due)));
        }
    }

    private void doCheckout() {
        PaymentMethod payment = selectedPayment();
        BigDecimal paid = null;
        if (payment == PaymentMethod.EFECTIVO && !paidWith.getText().isBlank()) {
            Optional<BigDecimal> parsed = ProductInputValidator.parsePositivePrice(paidWith.getText());
            if (parsed.isEmpty()) {
                feedback.setText("El importe recibido no es válido.");
                return;
            }
            paid = parsed.get();
        }
        CondicionIva condicion = (CondicionIva) buyerType.getSelectedItem();
        Buyer buyer = condicion == CondicionIva.CONSUMIDOR_FINAL ? Buyer.CONSUMIDOR_FINAL
                : new Buyer(condicion, buyerName.getText(), buyerCuit.getText());
        RegisterSale.Result result = services.registerSale().checkout(
                new RegisterSale.Checkout(cart.lines(), cart.discountPercent(), payment, buyer, paid));
        if (result instanceof RegisterSale.Result.Rejected rejected) {
            feedback.setText(rejected.message());
            return;
        }
        var sale = ((RegisterSale.Result.Completed) result).sale();
        cart.clear();
        discount.setValue(0.0);
        paidWith.setText("");
        buyerType.setSelectedItem(CondicionIva.CONSUMIDOR_FINAL);
        buyerCuit.setText("");
        buyerName.setText("");
        payments.get(PaymentMethod.EFECTIVO).setSelected(true);
        updatePayment();
        feedback.setText(" ");
        services.events().publish();
        services.dialogs().show("Venta " + sale.formattedNumber(),
                close -> new TicketPanel(sale, services.store().profile(), close, true));
        focusSearch();
    }

    // ---- componentes auxiliares ---------------------------------------------------------------

    private final class LineRow extends JPanel {
        LineRow(SaleLine line) {
            super(new GridBagLayout());
            setBackground(Theme.SURFACE);
            setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF1F3F5)),
                    BorderFactory.createEmptyBorder(10, 16, 10, 8)));
            setAlignmentX(LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
            ProductView product = cart.product(line.productId()).orElseThrow();

            JLabel thumb = new JLabel(new javax.swing.ImageIcon(ProductImages.image(product.image(), 40)));
            JLabel name = Theme.label(line.name(), Font.BOLD, 13f, Theme.TEXT);
            name.setToolTipText(line.name());
            name.setPreferredSize(new Dimension(60, 18));
            JLabel unit = Theme.muted(Pesos.format(line.unitPrice()) + " c/u");

            JButton minus = stepButton("−", "Quitar una unidad de " + line.name());
            JButton plus = stepButton("+", "Agregar una unidad de " + line.name());
            JTextField qty = new JTextField(Integer.toString(line.quantity()), 3);
            qty.setHorizontalAlignment(SwingConstants.CENTER);
            qty.setName("Cantidad " + line.name());
            qty.getAccessibleContext().setAccessibleName("Cantidad de " + line.name());
            qty.putClientProperty(FlatClientProperties.STYLE, "margin: 2,2,2,2");
            minus.addActionListener(event -> change(product, line.quantity() - 1));
            plus.addActionListener(event -> change(product, line.quantity() + 1));
            qty.addActionListener(event -> {
                try {
                    change(product, Integer.parseInt(qty.getText().trim()));
                } catch (NumberFormatException exception) {
                    feedback.setText("La cantidad debe ser un número entero.");
                }
            });
            JPanel stepper = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
            stepper.setOpaque(false);
            stepper.add(minus);
            stepper.add(qty);
            stepper.add(plus);

            JLabel lineTotal = Theme.label(Pesos.format(line.total()), Font.BOLD, 13.5f, Theme.TEXT);
            lineTotal.setHorizontalAlignment(SwingConstants.RIGHT);
            JButton remove = Theme.link("✕");
            remove.setForeground(Theme.MUTED);
            remove.setToolTipText("Quitar del ticket");
            remove.getAccessibleContext().setAccessibleName("Quitar " + line.name());
            remove.addActionListener(event -> change(product, 0));

            GridBagConstraints c = new GridBagConstraints();
            c.gridheight = 2;
            c.insets = new Insets(0, 0, 0, 10);
            add(thumb, c);
            c.gridheight = 1;
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 0, 0);
            add(name, c);
            c.gridy = 1;
            add(unit, c);
            c.gridx = 2;
            c.gridy = 0;
            c.gridheight = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            add(stepper, c);
            c.gridx = 3;
            c.insets = new Insets(0, 8, 0, 0);
            lineTotal.setPreferredSize(new Dimension(92, 20));
            add(lineTotal, c);
            c.gridx = 4;
            c.insets = new Insets(0, 2, 0, 0);
            add(remove, c);
        }

        private void change(ProductView product, int quantity) {
            Optional<String> problem = cart.setQuantity(product, quantity);
            feedback.setText(problem.orElse(" "));
            refreshTicket();
            renderGrid();
        }
    }

    private static JButton stepButton(String text, String accessibleName) {
        JButton button = new JButton(text);
        button.setFont(Theme.font(Font.BOLD, 14f));
        button.setFocusable(false);
        button.getAccessibleContext().setAccessibleName(accessibleName);
        button.putClientProperty(FlatClientProperties.STYLE, "arc: 999; margin: 0,7,0,7; background: #F1F3F5; borderWidth: 0");
        return button;
    }

    private static JLabel amountLabel(boolean bold) {
        JLabel label = Theme.label(" ", bold ? Font.BOLD : Font.PLAIN, 13f, Theme.TEXT);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private static JLabel amount(String text) {
        JLabel label = amountLabel(false);
        label.setText(text);
        return label;
    }

    private static JPanel row(JComponent left, JComponent right) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    static DocumentListener onChange(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                action.run();
            }
        };
    }

    /** Panel que sigue el ancho del scroll para que la grilla reacomode columnas. */
    private static final class GridPanel extends JPanel implements Scrollable {
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
            return false;
        }
    }
}
