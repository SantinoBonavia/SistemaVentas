package com.sistemaventas.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.sistemaventas.sales.application.Pesos;
import com.sistemaventas.sales.application.SalesReport;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.Timer;

/** Ventana principal sin dependencia de pantalla: el JFrame la crea {@code Main}. */
public final class MainFrame {
    public enum Section {
        VENDER("Vender", "F1"), PRODUCTOS("Productos", "F2"), VENTAS("Ventas", "F3"), CONFIGURACION("Configuración", "F4");

        final String title;
        final String key;

        Section(String title, String key) {
            this.title = title;
            this.key = key;
        }
    }

    private final Services services;
    private final JPanel root = new JPanel(new BorderLayout());
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<Section, JToggleButton> nav = new LinkedHashMap<>();
    private final JLabel storeName = Theme.label(" ", Font.BOLD, 17f, Color.WHITE);
    private final JLabel today = Theme.label(" ", Font.BOLD, 14f, Color.WHITE);
    private final JLabel todayDetail = Theme.label(" ", Font.PLAIN, 11.5f, Theme.HEADER_MUTED);
    private final JLabel clock = Theme.label(" ", Font.PLAIN, 12.5f, Theme.HEADER_MUTED);
    private final PosPanel pos;
    private Section current = Section.VENDER;

    public MainFrame(Services services) {
        this.services = services;
        pos = new PosPanel(services);
        content.add(pos, Section.VENDER.name());
        content.add(new InventoryPanel(services), Section.PRODUCTOS.name());
        content.add(new SalesPanel(services), Section.VENTAS.name());
        content.add(new SettingsPanel(services), Section.CONFIGURACION.name());
        root.setBackground(Theme.BACKGROUND);
        root.setFocusable(true);
        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(content, BorderLayout.CENTER);
        bindKeys();
        services.events().subscribe(this::refreshHeader);
        refreshHeader();
        Timer timer = new Timer(15_000, event -> tick());
        timer.setInitialDelay(0);
        timer.start();
        tick();
        show(Section.VENDER);
    }

    public JPanel component() {
        return root;
    }

    public Section current() {
        return current;
    }

    public void show(Section section) {
        current = section;
        cards.show(content, section.name());
        nav.get(section).setSelected(true);
        if (section == Section.VENDER) pos.focusSearch();
    }

    PosPanel pos() {
        return pos;
    }

    JToggleButton navButton(Section section) {
        return nav.get(section);
    }

    JLabel todayLabel() {
        return today;
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel(new BorderLayout(24, 0));
        header.setBackground(Theme.HEADER);
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        header.setPreferredSize(new Dimension(10, 66));

        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        brand.add(new Logo(), BorderLayout.WEST);
        JPanel names = new JPanel(new GridLayout(2, 1));
        names.setOpaque(false);
        names.add(storeName);
        names.add(Theme.label("Sistema de ventas · Punto de venta", Font.PLAIN, 11.5f, Theme.HEADER_MUTED));
        brand.add(names, BorderLayout.CENTER);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 6));
        tabs.setOpaque(false);
        ButtonGroup group = new ButtonGroup();
        for (Section section : Section.values()) {
            JToggleButton button = new JToggleButton("<html>" + section.title + " <span style='color:#7d8590;font-size:9px'>"
                    + section.key + "</span></html>");
            button.setName(section.title);
            button.getAccessibleContext().setAccessibleName(section.title);
            button.setFont(Theme.font(Font.BOLD, 14f));
            button.setForeground(new Color(0xD0D7DE));
            button.setFocusable(false);
            button.putClientProperty(FlatClientProperties.STYLE,
                    "arc: 10; borderWidth: 0; focusWidth: 0; background: #1F2328; hoverBackground: #2D333B; "
                            + "selectedBackground: #E8590C; selectedForeground: #FFFFFF; pressedBackground: #373E47; margin: 6,14,6,14");
            button.addActionListener(event -> show(section));
            group.add(button);
            nav.put(section, button);
            tabs.add(button);
        }

        JPanel status = new JPanel(new FlowLayout(FlowLayout.TRAILING, 18, 4));
        status.setOpaque(false);
        JPanel todayBox = new JPanel(new GridLayout(2, 1));
        todayBox.setOpaque(false);
        today.setHorizontalAlignment(JLabel.RIGHT);
        todayDetail.setHorizontalAlignment(JLabel.RIGHT);
        todayBox.add(today);
        todayBox.add(todayDetail);
        today.setName("Ventas de hoy");
        status.add(todayBox);
        status.add(clock);

        JPanel left = new JPanel(new BorderLayout(28, 0));
        left.setOpaque(false);
        left.add(brand, BorderLayout.WEST);
        left.add(tabs, BorderLayout.CENTER);
        header.add(left, BorderLayout.WEST);
        header.add(status, BorderLayout.EAST);
        return header;
    }

    private void refreshHeader() {
        storeName.setText(services.store().profile().name());
        SalesReport.Summary summary = services.report().between(LocalDate.now(services.clock()), null);
        today.setText("Hoy " + Pesos.compact(summary.total()));
        todayDetail.setText(summary.count() == 1 ? "1 venta" : summary.count() + " ventas");
    }

    private void tick() {
        String text = LocalDateTime.now(services.clock())
                .format(DateTimeFormatter.ofPattern("EEE dd/MM/yyyy · HH:mm", Locale.forLanguageTag("es-AR")));
        clock.setText(Character.toUpperCase(text.charAt(0)) + text.substring(1));
    }

    private void bindKeys() {
        int[] keys = {KeyEvent.VK_F1, KeyEvent.VK_F2, KeyEvent.VK_F3, KeyEvent.VK_F4};
        Section[] sections = Section.values();
        for (int i = 0; i < keys.length; i++) {
            Section section = sections[i];
            root.registerKeyboardAction(event -> show(section), KeyStroke.getKeyStroke(keys[i], 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }
        root.registerKeyboardAction(event -> {
            show(Section.VENDER);
            pos.focusSearch();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        root.registerKeyboardAction(event -> {
            if (current == Section.VENDER) pos.checkout();
        }, KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Llave sobre un cuadrado naranja redondeado. */
    private static final class Logo extends JComponent {
        private final java.awt.Image icon = ToolArt.image("llave", 34);

        Logo() {
            setPreferredSize(new Dimension(42, 42));
        }

        @Override
        protected void paintComponent(java.awt.Graphics graphics) {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int y = (getHeight() - 42) / 2;
            g.setColor(Theme.ACCENT);
            g.fillRoundRect(0, y, 42, 42, 12, 12);
            g.drawImage(icon, 4, y + 4, null);
            g.dispose();
        }
    }
}
