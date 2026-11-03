package com.sistemaventas.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

/** Paleta y tipografía de la aplicación: mostrador de ferretería, naranja sobre gris carbón. */
public final class Theme {
    public static final Color ACCENT = new Color(0xE8590C);
    public static final Color ACCENT_DARK = new Color(0xC2410C);
    public static final Color ACCENT_SOFT = new Color(0xFFF4E6);
    public static final Color HEADER = new Color(0x1F2328);
    public static final Color HEADER_MUTED = new Color(0x9AA4AF);
    public static final Color BACKGROUND = new Color(0xF3F4F6);
    public static final Color SURFACE = Color.WHITE;
    public static final Color BORDER = new Color(0xE5E7EB);
    public static final Color TEXT = new Color(0x1F2328);
    public static final Color MUTED = new Color(0x6B7280);
    public static final Color SUCCESS = new Color(0x2F9E44);
    public static final Color SUCCESS_SOFT = new Color(0xEBFBEE);
    public static final Color WARNING = new Color(0xB35C00);
    public static final Color WARNING_SOFT = new Color(0xFFF3BF);
    public static final Color DANGER = new Color(0xC92A2A);
    public static final Color DANGER_SOFT = new Color(0xFFE3E3);

    private Theme() {
    }

    /** Instala FlatLaf con el color de acento; llamar antes de crear componentes. */
    public static void install() {
        FlatLaf.setGlobalExtraDefaults(Map.of("@accentColor", "#E8590C", "@background", "#F3F4F6"));
        FlatLightLaf.setup();
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        UIManager.put("Table.rowHeight", 56);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
        UIManager.put("Table.selectionBackground", new Color(0xFFE8CC));
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.selectionInactiveBackground", new Color(0xFFF0DE));
        UIManager.put("Table.selectionInactiveForeground", TEXT);
        UIManager.put("TableHeader.height", 36);
        UIManager.put("TabbedPane.selectedBackground", SURFACE);
        UIManager.put("TitlePane.unifiedBackground", false);
    }

    public static Font font(int style, float size) {
        Font base = UIManager.getFont("Label.font");
        if (base == null) base = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        return base.deriveFont(style, size);
    }

    public static Font mono(float size) {
        return new Font(Font.MONOSPACED, Font.PLAIN, Math.round(size));
    }

    public static JLabel label(String text, int style, float size, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font(style, size));
        label.setForeground(color);
        return label;
    }

    public static JLabel title(String text) {
        return label(text, Font.BOLD, 22f, TEXT);
    }

    public static JLabel muted(String text) {
        return label(text, Font.PLAIN, 12.5f, MUTED);
    }

    /** Panel blanco con borde redondeado suave. */
    public static JPanel card() {
        JPanel panel = new JPanel();
        panel.setBackground(SURFACE);
        panel.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(BORDER, 14), BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        return panel;
    }

    public static JButton primary(String text) {
        JButton button = new JButton(text);
        button.setBackground(ACCENT);
        button.setForeground(Color.WHITE);
        button.setFont(font(Font.BOLD, 13.5f));
        button.putClientProperty(FlatClientProperties.STYLE,
                "borderWidth: 0; focusWidth: 0; hoverBackground: #C2410C; pressedBackground: #9A3412; disabledBackground: #F3C3A5; disabledText: #FFFFFF");
        return button;
    }

    public static JButton success(String text) {
        JButton button = new JButton(text);
        button.setBackground(SUCCESS);
        button.setForeground(Color.WHITE);
        button.setFont(font(Font.BOLD, 17f));
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 14; borderWidth: 0; focusWidth: 0; hoverBackground: #2B8A3E; pressedBackground: #237032; disabledBackground: #B2DDBA; disabledText: #FFFFFF");
        return button;
    }

    public static JButton secondary(String text) {
        JButton button = new JButton(text);
        button.setFont(font(Font.PLAIN, 13.5f));
        button.putClientProperty(FlatClientProperties.STYLE, "background: #FFFFFF; borderColor: #D1D5DB; focusWidth: 0");
        return button;
    }

    public static JButton danger(String text) {
        JButton button = secondary(text);
        button.setForeground(DANGER);
        return button;
    }

    /** Botón sin borde, para acciones secundarias dentro de una tarjeta. */
    public static JButton link(String text) {
        JButton button = new JButton(text);
        button.setFont(font(Font.PLAIN, 12.5f));
        button.setForeground(ACCENT_DARK);
        button.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        button.setFocusable(false);
        return button;
    }

    /** Botón tipo "chip" seleccionable (categorías, períodos, medios de pago). */
    public static void chip(AbstractButton button) {
        button.setFont(font(Font.PLAIN, 13f));
        button.setFocusable(false);
        button.putClientProperty(FlatClientProperties.STYLE,
                "arc: 999; background: #FFFFFF; borderColor: #D1D5DB; selectedBackground: #1F2328; selectedForeground: #FFFFFF; focusWidth: 0; margin: 5,14,5,14");
    }

    public static void placeholder(JComponent field, String text) {
        field.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, text);
    }

    public static Color tint(String category) {
        Color[] tints = {new Color(0xFFF4E6), new Color(0xE7F5FF), new Color(0xEBFBEE), new Color(0xFFF0F6),
                new Color(0xF3F0FF), new Color(0xFFF9DB), new Color(0xE6FCF5), new Color(0xF1F3F5)};
        return tints[Math.floorMod(category == null ? 0 : category.hashCode(), tints.length)];
    }
}
