package com.sistemaventas.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.sistemaventas.inventory.application.DeleteResult;
import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.inventory.application.ReceiveStock;
import com.sistemaventas.sales.application.Pesos;
import com.sistemaventas.tax.AlicuotaIva;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

/** Inventario: alta, edición, ingreso de mercadería y baja de productos. */
public final class InventoryPanel extends JPanel {
    private static final String ALL = "Todas las categorías";
    private final Services services;
    private final JTextField search = new JTextField(22);
    private final JComboBox<String> categoryFilter = new JComboBox<>();
    private final JCheckBox lowOnly = new JCheckBox("Solo stock bajo o sin stock");
    private final ProductsModel model = new ProductsModel();
    private final JTable table = new JTable(model);
    private final JButton edit = Theme.secondary("Editar");
    private final JButton receive = Theme.secondary("Ingresar stock");
    private final JButton delete = Theme.danger("Eliminar");
    private final JLabel countStat = stat();
    private final JLabel valueStat = stat();
    private final JLabel lowStat = stat();
    private final JLabel outStat = stat();
    private List<ProductView> all = List.of();
    private boolean reloading;

    public InventoryPanel(Services services) {
        super(new BorderLayout(0, 14));
        this.services = services;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildTable(), BorderLayout.CENTER);
        services.events().subscribe(this::reload);
        reload();
    }

    JTable table() { return table; }
    JTextField searchField() { return search; }
    JCheckBox lowOnlyFilter() { return lowOnly; }
    List<ProductView> visibleProducts() { return model.rows; }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 14));
        header.setOpaque(false);

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(Theme.title("Productos"));
        titles.add(Theme.muted("Precios finales con IVA incluido. Doble clic en un producto para editarlo."));
        titleRow.add(titles, BorderLayout.WEST);
        JButton add = Theme.primary("+  Nuevo producto");
        add.setName("Nuevo producto");
        add.setPreferredSize(new Dimension(180, 40));
        add.addActionListener(event -> openForm(null));
        JPanel addBox = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        addBox.setOpaque(false);
        addBox.add(add);
        titleRow.add(addBox, BorderLayout.EAST);

        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.add(statCard("Productos", countStat, Theme.TEXT));
        stats.add(statCard("Valor del inventario (precio de venta)", valueStat, Theme.TEXT));
        stats.add(statCard("Con stock bajo", lowStat, Theme.WARNING));
        stats.add(statCard("Sin stock", outStat, Theme.DANGER));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setOpaque(false);
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
        filters.setOpaque(false);
        search.setName("Buscar en inventario");
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_ICON, new com.formdev.flatlaf.icons.FlatSearchIcon());
        search.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        Theme.placeholder(search, "Nombre o código");
        search.getDocument().addDocumentListener(PosPanel.onChange(this::applyFilters));
        categoryFilter.setName("Filtrar por categoría");
        categoryFilter.addActionListener(event -> {
            if (!reloading) applyFilters();
        });
        lowOnly.setOpaque(false);
        lowOnly.addActionListener(event -> applyFilters());
        filters.add(search);
        filters.add(categoryFilter);
        filters.add(lowOnly);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.setOpaque(false);
        edit.addActionListener(event -> selected().ifPresent(this::openForm));
        receive.addActionListener(event -> selected().ifPresent(this::openStock));
        delete.addActionListener(event -> selected().ifPresent(this::confirmDelete));
        actions.add(edit);
        actions.add(receive);
        actions.add(delete);
        toolbar.add(filters, BorderLayout.WEST);
        toolbar.add(actions, BorderLayout.EAST);

        header.add(titleRow, BorderLayout.NORTH);
        header.add(stats, BorderLayout.CENTER);
        header.add(toolbar, BorderLayout.SOUTH);
        return header;
    }

    private JComponent buildTable() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(58);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xF1F3F5));
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 12.5f));
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(event -> updateButtons());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getClickCount() == 2) selected().ifPresent(InventoryPanel.this::openForm);
            }
        });
        TableColumnModel columns = table.getColumnModel();
        int[] widths = {64, 90, 320, 170, 110, 70, 110, 70, 60, 110};
        for (int i = 0; i < widths.length; i++) columns.getColumn(i).setPreferredWidth(widths[i]);
        columns.getColumn(0).setMaxWidth(70);
        columns.getColumn(0).setCellRenderer(new ImageRenderer());
        DefaultTableCellRenderer right = new PaddedRenderer(SwingConstants.RIGHT, false);
        DefaultTableCellRenderer rightBold = new PaddedRenderer(SwingConstants.RIGHT, true);
        DefaultTableCellRenderer left = new PaddedRenderer(SwingConstants.LEFT, false);
        DefaultTableCellRenderer leftBold = new PaddedRenderer(SwingConstants.LEFT, true);
        columns.getColumn(1).setCellRenderer(left);
        columns.getColumn(2).setCellRenderer(leftBold);
        columns.getColumn(3).setCellRenderer(left);
        columns.getColumn(4).setCellRenderer(right);
        columns.getColumn(5).setCellRenderer(right);
        columns.getColumn(6).setCellRenderer(rightBold);
        columns.getColumn(7).setCellRenderer(rightBold);
        columns.getColumn(8).setCellRenderer(right);
        columns.getColumn(9).setCellRenderer(new StatusRenderer());
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new RoundedBorder(Theme.BORDER, 14));
        scroll.getViewport().setBackground(Theme.SURFACE);
        return scroll;
    }

    private void reload() {
        all = services.browse().list();
        reloading = true;
        Object current = categoryFilter.getSelectedItem();
        categoryFilter.removeAllItems();
        categoryFilter.addItem(ALL);
        all.stream().map(ProductView::categoryName).distinct().sorted().forEach(categoryFilter::addItem);
        categoryFilter.setSelectedItem(current == null ? ALL : current);
        reloading = false;

        countStat.setText(Integer.toString(all.size()));
        BigDecimal value = all.stream().map(product -> product.price().multiply(BigDecimal.valueOf(product.stock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        valueStat.setText(Pesos.compact(value));
        lowStat.setText(Long.toString(all.stream().filter(p -> p.stockStatus() == ProductView.StockStatus.BAJO).count()));
        outStat.setText(Long.toString(all.stream().filter(p -> p.stockStatus() == ProductView.StockStatus.SIN_STOCK).count()));
        applyFilters();
    }

    private void applyFilters() {
        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        Object category = categoryFilter.getSelectedItem();
        long selectedId = selected().map(ProductView::id).orElse(-1L);
        model.setRows(all.stream()
                .filter(p -> query.isEmpty() || p.name().toLowerCase(Locale.ROOT).contains(query)
                        || p.code().toLowerCase(Locale.ROOT).contains(query))
                .filter(p -> category == null || ALL.equals(category) || p.categoryName().equals(category))
                .filter(p -> !lowOnly.isSelected() || p.stockStatus() != ProductView.StockStatus.DISPONIBLE)
                .toList());
        for (int row = 0; row < model.rows.size(); row++) {
            if (model.rows.get(row).id() == selectedId) {
                int view = table.convertRowIndexToView(row);
                if (view >= 0) table.setRowSelectionInterval(view, view);
            }
        }
        updateButtons();
    }

    private java.util.Optional<ProductView> selected() {
        int view = table.getSelectedRow();
        if (view < 0) return java.util.Optional.empty();
        int row = table.convertRowIndexToModel(view);
        return row < model.rows.size() ? java.util.Optional.of(model.rows.get(row)) : java.util.Optional.empty();
    }

    void select(int row) {
        table.setRowSelectionInterval(row, row);
    }

    private void updateButtons() {
        boolean any = table.getSelectedRow() >= 0;
        edit.setEnabled(any);
        receive.setEnabled(any);
        delete.setEnabled(any);
    }

    void openForm(ProductView product) {
        services.dialogs().show(product == null ? "Nuevo producto" : "Editar producto",
                close -> new ProductFormPanel(services, product, close));
    }

    void openStock(ProductView product) {
        services.dialogs().show("Movimiento de stock", close -> new StockPanel(product, close));
    }

    void confirmDelete(ProductView product) {
        if (!services.dialogs().confirm("Eliminar producto",
                "¿Eliminar \"" + product.name() + "\" del catálogo?\nLas ventas ya registradas no se modifican.", "Eliminar")) {
            return;
        }
        DeleteResult result = services.delete().delete(product.id());
        if (result instanceof DeleteResult.ConfirmationRequired required) {
            result = services.delete().confirm(required.confirmation().token());
        }
        if (result instanceof DeleteResult.Failure failure) {
            services.dialogs().message("Eliminar producto", failure.message());
        }
        services.events().publish();
    }

    // ---- ingreso / ajuste de stock ------------------------------------------------------------

    private final class StockPanel extends JPanel {
        StockPanel(ProductView product, Runnable close) {
            super(new BorderLayout(0, 12));
            setBackground(Theme.BACKGROUND);
            setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));
            JPanel head = new JPanel(new BorderLayout(12, 0));
            head.setOpaque(false);
            head.add(new JLabel(new ImageIcon(ProductImages.image(product.image(), 64))), BorderLayout.WEST);
            JPanel titles = new JPanel(new GridLayout(2, 1));
            titles.setOpaque(false);
            titles.add(Theme.label(product.name(), Font.BOLD, 16f, Theme.TEXT));
            titles.add(Theme.muted("Stock actual: " + product.stock() + " unidades · mínimo " + product.minStock()));
            head.add(titles, BorderLayout.CENTER);
            add(head, BorderLayout.NORTH);

            JPanel card = Theme.card();
            card.setLayout(new GridLayout(0, 1, 0, 8));
            JRadioButton in = new JRadioButton("Ingreso de mercadería (suma)", true);
            JRadioButton out = new JRadioButton("Ajuste por rotura, pérdida o conteo (resta)");
            javax.swing.ButtonGroup group = new javax.swing.ButtonGroup();
            group.add(in);
            group.add(out);
            in.setOpaque(false);
            out.setOpaque(false);
            JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, 1, 100000, 1));
            quantity.setName("Cantidad");
            JLabel result = Theme.muted(" ");
            Runnable preview = () -> {
                int delta = (Integer) quantity.getValue() * (in.isSelected() ? 1 : -1);
                result.setText("Quedarán " + (product.stock() + delta) + " unidades.");
            };
            quantity.addChangeListener(event -> preview.run());
            in.addActionListener(event -> preview.run());
            out.addActionListener(event -> preview.run());
            preview.run();
            card.add(in);
            card.add(out);
            JPanel qtyRow = new JPanel(new BorderLayout(8, 0));
            qtyRow.setOpaque(false);
            qtyRow.add(Theme.muted("Cantidad"), BorderLayout.WEST);
            qtyRow.add(quantity, BorderLayout.CENTER);
            card.add(qtyRow);
            card.add(result);
            add(card, BorderLayout.CENTER);

            JLabel feedback = Theme.label(" ", Font.PLAIN, 12.5f, Theme.DANGER);
            JButton cancel = Theme.secondary("Cancelar");
            cancel.addActionListener(event -> close.run());
            JButton apply = Theme.primary("Confirmar");
            apply.addActionListener(event -> {
                int delta = (Integer) quantity.getValue() * (in.isSelected() ? 1 : -1);
                ReceiveStock.Result outcome = services.receiveStock().adjust(product.id(), delta);
                if (outcome instanceof ReceiveStock.Result.Rejected rejected) {
                    feedback.setText(rejected.message());
                } else {
                    services.events().publish();
                    close.run();
                }
            });
            JPanel bar = new JPanel(new BorderLayout());
            bar.setOpaque(false);
            bar.add(feedback, BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
            buttons.setOpaque(false);
            buttons.add(cancel);
            buttons.add(apply);
            bar.add(buttons, BorderLayout.EAST);
            add(bar, BorderLayout.SOUTH);
            setPreferredSize(new Dimension(460, 330));
        }
    }

    // ---- tabla --------------------------------------------------------------------------------

    private static final class ProductsModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"", "Código", "Producto", "Categoría", "Precio sin IVA", "IVA",
                "Precio final", "Stock", "Mín.", "Estado"};
        private List<ProductView> rows = List.of();

        void setRows(List<ProductView> rows) {
            this.rows = List.copyOf(rows);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return switch (column) {
                case 4, 6 -> BigDecimal.class;
                case 7, 8 -> Integer.class;
                default -> Object.class;
            };
        }

        @Override
        public Object getValueAt(int row, int column) {
            ProductView product = rows.get(row);
            return switch (column) {
                case 0 -> product.image();
                case 1 -> product.code();
                case 2 -> product.name();
                case 3 -> product.categoryName();
                case 4 -> product.netPrice();
                case 5 -> AlicuotaIva.fromPercent(product.ivaPercent()).map(AlicuotaIva::label).orElse("");
                case 6 -> product.price();
                case 7 -> product.stock();
                case 8 -> product.minStock();
                default -> product.stockStatus();
            };
        }
    }

    private static class PaddedRenderer extends DefaultTableCellRenderer {
        private final boolean bold;

        PaddedRenderer(int alignment, boolean bold) {
            setHorizontalAlignment(alignment);
            this.bold = bold;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            Object shown = value instanceof BigDecimal amount ? Pesos.format(amount) : value;
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, shown, selected, false, row, column);
            label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 12));
            label.setFont(Theme.font(bold ? Font.BOLD : Font.PLAIN, 13f));
            return label;
        }
    }

    private static final class ImageRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, "", selected, false, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setIcon(new ImageIcon(ProductImages.image((String) value, 44)));
            return label;
        }
    }

    private static final class StatusRenderer extends DefaultTableCellRenderer {
        private ProductView.StockStatus status;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, "", selected, false, row, column);
            status = (ProductView.StockStatus) value;
            getAccessibleContext().setAccessibleName(text(status));
            return this;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            String text = text(status);
            Color background = switch (status) {
                case SIN_STOCK -> Theme.DANGER_SOFT;
                case BAJO -> Theme.WARNING_SOFT;
                default -> Theme.SUCCESS_SOFT;
            };
            Color foreground = switch (status) {
                case SIN_STOCK -> Theme.DANGER;
                case BAJO -> Theme.WARNING;
                default -> Theme.SUCCESS;
            };
            g.setFont(Theme.font(Font.BOLD, 11.5f));
            int width = g.getFontMetrics().stringWidth(text) + 18;
            int y = (getHeight() - 22) / 2;
            g.setColor(background);
            g.fillRoundRect(8, y, width, 22, 22, 22);
            g.setColor(foreground);
            g.drawString(text, 17, y + 15);
            g.dispose();
        }

        private static String text(ProductView.StockStatus status) {
            return switch (status) {
                case SIN_STOCK -> "Sin stock";
                case BAJO -> "Stock bajo";
                case DISPONIBLE -> "Disponible";
                case null -> "";
            };
        }
    }

    // ---- tarjetas de resumen ------------------------------------------------------------------

    private static JLabel stat() {
        return Theme.label("0", Font.BOLD, 22f, Theme.TEXT);
    }

    static JPanel statCard(String title, JLabel value, Color color) {
        JPanel card = Theme.card();
        card.setLayout(new BorderLayout(0, 2));
        card.add(Theme.muted(title), BorderLayout.NORTH);
        value.setForeground(color);
        card.add(value, BorderLayout.CENTER);
        return card;
    }
}
