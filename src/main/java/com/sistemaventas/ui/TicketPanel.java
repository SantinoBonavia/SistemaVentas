package com.sistemaventas.ui;

import com.sistemaventas.sales.application.Pesos;
import com.sistemaventas.sales.application.SaleRecord;
import com.sistemaventas.sales.application.TicketFormatter;
import com.sistemaventas.store.StoreProfile;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.print.PrinterException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/** Vista del comprobante con opción de imprimirlo. */
final class TicketPanel extends JPanel {
    private final JTextArea paper = new JTextArea();

    TicketPanel(SaleRecord sale, StoreProfile store, Runnable close, boolean justSold) {
        super(new BorderLayout(0, 12));
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));

        if (justSold) {
            JPanel banner = new JPanel(new BorderLayout(0, 2));
            banner.setBackground(Theme.SUCCESS_SOFT);
            banner.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(new Color(0xB2F2BB), 12),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));
            banner.add(Theme.label("✔  Venta registrada", Font.BOLD, 16f, Theme.SUCCESS), BorderLayout.NORTH);
            String detail = "Total " + Pesos.format(sale.totals().total()) + " · " + sale.payment().label();
            if (sale.change() != null) detail += " · Vuelto " + Pesos.format(sale.change());
            JLabel info = Theme.label(detail, Font.PLAIN, 14f, Theme.TEXT);
            info.setName("Resumen de la venta");
            banner.add(info, BorderLayout.CENTER);
            add(banner, BorderLayout.NORTH);
        }

        paper.setText(TicketFormatter.format(sale, store));
        paper.setFont(Theme.mono(13f));
        paper.setEditable(false);
        paper.setBackground(new Color(0xFFFEF8));
        paper.setForeground(Theme.TEXT);
        paper.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        paper.setCaretPosition(0);
        paper.getAccessibleContext().setAccessibleName("Ticket");
        JScrollPane scroll = new JScrollPane(paper);
        scroll.setBorder(new RoundedBorder(Theme.BORDER, 10));
        scroll.setPreferredSize(new Dimension(paper.getPreferredSize().width + 30, Math.min(600, paper.getPreferredSize().height + 6)));
        add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        actions.setOpaque(false);
        JButton print = Theme.secondary("Imprimir");
        print.addActionListener(event -> {
            try {
                paper.print();
            } catch (PrinterException exception) {
                javax.swing.JOptionPane.showMessageDialog(this, "No se pudo imprimir: " + exception.getMessage());
            }
        });
        JButton done = Theme.primary(justSold ? "Nueva venta" : "Cerrar");
        done.addActionListener(event -> close.run());
        actions.add(print);
        actions.add(done);
        add(actions, BorderLayout.SOUTH);
        addHierarchyListener(event -> {
            if (isShowing()) done.requestFocusInWindow();
        });
    }

    String text() {
        return paper.getText();
    }
}
