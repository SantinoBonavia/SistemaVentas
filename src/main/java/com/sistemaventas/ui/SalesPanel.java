package com.sistemaventas.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.sistemaventas.sales.application.PaymentMethod;
import com.sistemaventas.sales.application.Pesos;
import com.sistemaventas.sales.application.RegisterSale;
import com.sistemaventas.sales.application.SaleRecord;
import com.sistemaventas.sales.application.SalesReport;
import com.sistemaventas.sales.application.TicketFormatter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** Historial de ventas con resumen del período, ticket y anulación. */
public final class SalesPanel extends JPanel {
    enum Period {
        HOY("Hoy"), SEMANA("Últimos 7 días"), MES("Este mes"), TODO("Todo");

        final String label;

        Period(String label) {
            this.label = label;
        }
    }

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final Services services;
    private Period period = Period.HOY;
    private final JLabel totalStat = stat();
    private final JLabel countStat = stat();
    private final JLabel averageStat = stat();
    private final JLabel ivaStat = stat();
    private final SalesModel model = new SalesModel();
    private final JTable table = new JTable(model);
    private final JTextArea ticket = new JTextArea();
    private final JPanel breakdown = new JPanel(new GridBagLayout());
    private final JButton print = Theme.secondary("Imprimir ticket");
    private final JButton cancel = Theme.danger("Anular venta");
    private SalesReport.Summary summary;

    public SalesPanel(Services services) {
        super(new BorderLayout(0, 14));
        this.services = services;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
        services.events().subscribe(this::reload);
        reload();
    }

    JTable table() { return table; }
    JLabel totalStat() { return totalStat; }
    JLabel countStat() { return countStat; }
    JButton cancelButton() { return cancel; }
    String ticketText() { return ticket.getText(); }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 14));
        header.setOpaque(false);
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JPanel titles = new JPanel(new GridLayout(2, 1));
        titles.setOpaque(false);
        titles.add(Theme.title("Ventas"));
        titles.add(Theme.muted("Comprobantes emitidos, totales del período e IVA débito fiscal."));
        titleRow.add(titles, BorderLayout.WEST);
        JPanel periods = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 0));
        periods.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (Period option : Period.values()) {
            JToggleButton chip = new JToggleButton(option.label, option == period);
            Theme.chip(chip);
            chip.addActionListener(event -> {
                period = option;
                reload();
            });
            group.add(chip);
            periods.add(chip);
        }
        titleRow.add(periods, BorderLayout.EAST);

        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.add(InventoryPanel.statCard("Total vendido", totalStat, Theme.TEXT));
        stats.add(InventoryPanel.statCard("Ventas", countStat, Theme.TEXT));
        stats.add(InventoryPanel.statCard("Ticket promedio", averageStat, Theme.TEXT));
        stats.add(InventoryPanel.statCard("IVA débito fiscal", ivaStat, Theme.ACCENT_DARK));
        header.add(titleRow, BorderLayout.NORTH);
        header.add(stats, BorderLayout.CENTER);
        return header;
    }

    private JComponent buildBody() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowHeight(40);
        table.setShowVerticalLines(false);
        table.setGridColor(new Color(0xF1F3F5));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setFont(Theme.font(Font.BOLD, 12.5f));
        table.getTableHeader().setReorderingAllowed(false);
        table.getSelectionModel().addListSelectionListener(event -> showSelected());
        int[] widths = {130, 130, 50, 170, 110, 50, 120, 90};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, false, row, column);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                boolean cancelled = model.rows.get(row).cancelled();
                label.setForeground(cancelled ? Theme.MUTED : column == 7 ? Theme.SUCCESS : Theme.TEXT);
                label.setFont(Theme.font(column == 6 ? Font.BOLD : Font.PLAIN, 13f));
                label.setHorizontalAlignment(column == 5 || column == 6 ? SwingConstants.RIGHT : SwingConstants.LEFT);
                if (cancelled && column == 7) label.setForeground(Theme.DANGER);
                return label;
            }
        };
        for (int i = 0; i < model.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(new RoundedBorder(Theme.BORDER, 14));
        tableScroll.getViewport().setBackground(Theme.SURFACE);

        ticket.setEditable(false);
        ticket.setFont(Theme.mono(12.5f));
        ticket.setBackground(new Color(0xFFFEF8));
        ticket.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        ticket.getAccessibleContext().setAccessibleName("Ticket seleccionado");
        JScrollPane ticketScroll = new JScrollPane(ticket);
        ticketScroll.setBorder(null);
        JPanel ticketTab = new JPanel(new BorderLayout(0, 8));
        ticketTab.setBackground(Theme.SURFACE);
        ticketTab.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        ticketTab.add(ticketScroll, BorderLayout.CENTER);
        JPanel actions = new JPanel(new GridLayout(1, 2, 8, 0));
        actions.setOpaque(false);
        print.addActionListener(event -> {
            try {
                ticket.print();
            } catch (java.awt.print.PrinterException exception) {
                services.dialogs().message("Imprimir", "No se pudo imprimir: " + exception.getMessage());
            }
        });
        cancel.setName("Anular venta");
        cancel.addActionListener(event -> cancelSelected());
        actions.add(print);
        actions.add(cancel);
        ticketTab.add(actions, BorderLayout.SOUTH);

        breakdown.setBackground(Theme.SURFACE);
        breakdown.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        JScrollPane breakdownScroll = new JScrollPane(breakdown);
        breakdownScroll.setBorder(null);

        JTabbedPane side = new JTabbedPane();
        side.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_TYPE, FlatClientProperties.TABBED_PANE_TAB_TYPE_UNDERLINED);
        side.addTab("Ticket", ticketTab);
        side.addTab("Resumen del período", breakdownScroll);
        JPanel sideBox = new JPanel(new BorderLayout());
        sideBox.setBackground(Theme.SURFACE);
        sideBox.setBorder(new RoundedBorder(Theme.BORDER, 14));
        sideBox.add(side);
        sideBox.setPreferredSize(new Dimension(420, 10));

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);
        body.add(tableScroll, BorderLayout.CENTER);
        body.add(sideBox, BorderLayout.EAST);
        return body;
    }

    private void reload() {
        LocalDate today = LocalDate.now(services.clock());
        LocalDate from = switch (period) {
            case HOY -> today;
            case SEMANA -> today.minusDays(6);
            case MES -> today.withDayOfMonth(1);
            case TODO -> null;
        };
        long selected = selectedSale().map(SaleRecord::number).orElse(-1L);
        summary = services.report().between(from, null);
        totalStat.setText(Pesos.compact(summary.total()));
        countStat.setText(Integer.toString(summary.count()));
        averageStat.setText(Pesos.compact(summary.average()));
        ivaStat.setText(Pesos.format(summary.iva()));
        model.setRows(summary.sales());
        int index = 0;
        for (int i = 0; i < model.rows.size(); i++) if (model.rows.get(i).number() == selected) index = i;
        if (!model.rows.isEmpty()) table.setRowSelectionInterval(index, index);
        showSelected();
        renderBreakdown();
    }

    private java.util.Optional<SaleRecord> selectedSale() {
        int row = table.getSelectedRow();
        return row < 0 || row >= model.rows.size() ? java.util.Optional.empty() : java.util.Optional.of(model.rows.get(row));
    }

    private void showSelected() {
        var sale = selectedSale();
        ticket.setText(sale.map(s -> TicketFormatter.format(s, services.store().profile()))
                .orElse("\n  No hay ventas en este período.\n\n  Las ventas que cobres en la pantalla\n  Vender aparecen acá."));
        ticket.setCaretPosition(0);
        print.setEnabled(sale.isPresent());
        cancel.setEnabled(sale.isPresent() && !sale.get().cancelled());
    }

    private void cancelSelected() {
        SaleRecord sale = selectedSale().orElse(null);
        if (sale == null) return;
        if (!services.dialogs().confirm("Anular venta", "¿Anular la venta " + sale.formattedNumber() + " por "
                + Pesos.format(sale.totals().total()) + "?\nLa mercadería vuelve al stock.", "Anular venta")) {
            return;
        }
        RegisterSale.Result result = services.cancelSale().cancel(sale.number());
        if (result instanceof RegisterSale.Result.Rejected rejected) {
            services.dialogs().message("Anular venta", rejected.message());
        }
        services.events().publish();
    }

    private void renderBreakdown() {
        breakdown.removeAll();
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 10, 0);
        breakdown.add(Theme.label("Por medio de pago", Font.BOLD, 14f, Theme.TEXT), c);
        BigDecimal max = summary.byPayment().values().stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
        c.insets = new Insets(0, 0, 8, 0);
        for (PaymentMethod method : PaymentMethod.values()) {
            BigDecimal amount = summary.byPayment().getOrDefault(method, BigDecimal.ZERO);
            breakdown.add(new Bar(method.label(), amount, max), c);
        }
        c.insets = new Insets(14, 0, 8, 0);
        breakdown.add(Theme.label("Más vendidos", Font.BOLD, 14f, Theme.TEXT), c);
        c.insets = new Insets(0, 0, 6, 0);
        if (summary.topProducts().isEmpty()) breakdown.add(Theme.muted("Sin ventas en el período."), c);
        int rank = 1;
        for (SalesReport.TopProduct product : summary.topProducts()) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            JLabel name = Theme.label(rank++ + ". " + product.name(), Font.PLAIN, 13f, Theme.TEXT);
            row.add(name, BorderLayout.CENTER);
            row.add(Theme.muted(product.quantity() + " u. · " + Pesos.compact(product.total())), BorderLayout.EAST);
            breakdown.add(row, c);
        }
        c.weighty = 1;
        breakdown.add(new JPanel() {{ setOpaque(false); }}, c);
        breakdown.revalidate();
        breakdown.repaint();
    }

    private static JLabel stat() {
        return Theme.label("$0", Font.BOLD, 22f, Theme.TEXT);
    }

    /** Barra horizontal simple con etiqueta y monto. */
    private static final class Bar extends JPanel {
        private final String label;
        private final BigDecimal amount;
        private final double ratio;

        Bar(String label, BigDecimal amount, BigDecimal max) {
            this.label = label;
            this.amount = amount;
            this.ratio = max.signum() == 0 ? 0 : amount.divide(max, MathContext.DECIMAL64).doubleValue();
            setOpaque(false);
            setPreferredSize(new Dimension(200, 38));
            getAccessibleContext().setAccessibleName(label + " " + Pesos.format(amount));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(Theme.font(Font.PLAIN, 12.5f));
            g.setColor(Theme.TEXT);
            g.drawString(label, 0, 14);
            String value = Pesos.format(amount);
            g.setFont(Theme.font(Font.BOLD, 12.5f));
            g.drawString(value, getWidth() - g.getFontMetrics().stringWidth(value), 14);
            g.setColor(new Color(0xF1F3F5));
            g.fillRoundRect(0, 22, getWidth(), 10, 10, 10);
            if (ratio > 0) {
                g.setColor(Theme.ACCENT);
                g.fillRoundRect(0, 22, Math.max(10, (int) (getWidth() * ratio)), 10, 10, 10);
            }
            g.dispose();
        }
    }

    private static final class SalesModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Comprobante", "Fecha", "Tipo", "Cliente", "Pago", "Art.", "Total", "Estado"};
        private List<SaleRecord> rows = List.of();

        void setRows(List<SaleRecord> rows) {
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
        public Object getValueAt(int row, int column) {
            SaleRecord sale = rows.get(row);
            return switch (column) {
                case 0 -> sale.formattedNumber();
                case 1 -> DATE_TIME.format(sale.dateTime());
                case 2 -> sale.letter();
                case 3 -> sale.buyer().displayName();
                case 4 -> sale.payment().label();
                case 5 -> sale.itemCount();
                case 6 -> Pesos.format(sale.totals().total());
                default -> sale.cancelled() ? "Anulada" : "Emitida";
            };
        }
    }
}
