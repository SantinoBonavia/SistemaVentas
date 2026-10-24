package com.sistemaventas.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.border.Border;

/** Shared visual defaults; controls always retain text labels in addition to color. */
public final class VintageTheme {
    public static final Color BACKGROUND = new Color(0xF3, 0xE9, 0xD2);
    public static final Color FOREGROUND = new Color(0x2B, 0x1D, 0x0E);
    public static final Color ACCENT = new Color(0x00, 0x5A, 0x9C);
    public static final Font BODY_FONT = new Font(Font.SERIF, Font.PLAIN, 16);
    public static final Font HEADING_FONT = new Font(Font.SERIF, Font.BOLD, 18);

    private VintageTheme() {
    }

    public static void applyTo(JComponent component) {
        component.setBackground(BACKGROUND);
        component.setForeground(FOREGROUND);
        component.setFont(BODY_FONT);
        component.setBorder(focusBorder());
        if (component instanceof AbstractButton button) button.setFocusPainted(true);
        if (component.getAccessibleContext().getAccessibleName() == null) {
            component.getAccessibleContext().setAccessibleName(component.getName());
        }
    }

    public static Border focusBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 2), BorderFactory.createEmptyBorder(6, 10, 6, 10)
        );
    }

    public static double contrastRatio(Color first, Color second) {
        double lighter = Math.max(luminance(first), luminance(second));
        double darker = Math.min(luminance(first), luminance(second));
        return (lighter + 0.05) / (darker + 0.05);
    }

    private static double luminance(Color color) {
        return 0.2126 * channel(color.getRed()) + 0.7152 * channel(color.getGreen()) + 0.0722 * channel(color.getBlue());
    }

    private static double channel(int value) {
        double normalized = value / 255.0;
        return normalized <= 0.03928 ? normalized / 12.92 : Math.pow((normalized + 0.055) / 1.055, 2.4);
    }
}
