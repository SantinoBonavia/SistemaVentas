package com.sistemaventas.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.border.AbstractBorder;

/** Borde de 1 px redondeado que además pinta el fondo de la esquina con el color del padre. */
final class RoundedBorder extends AbstractBorder {
    private final Color color;
    private final int arc;

    RoundedBorder(Color color, int arc) {
        this.color = color;
        this.arc = arc;
    }

    @Override
    public void paintBorder(Component component, Graphics graphics, int x, int y, int width, int height) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        java.awt.geom.Area corners = new java.awt.geom.Area(new java.awt.Rectangle(x, y, width, height));
        corners.subtract(new java.awt.geom.Area(new RoundRectangle2D.Double(x, y, width - 1, height - 1, arc, arc)));
        Component parent = component.getParent();
        g.setColor(parent == null ? Theme.BACKGROUND : parent.getBackground());
        g.fill(corners);
        g.setColor(color);
        g.setStroke(new BasicStroke(1f));
        g.draw(new RoundRectangle2D.Double(x + 0.5, y + 0.5, width - 2, height - 2, arc, arc));
        g.dispose();
    }

    @Override
    public Insets getBorderInsets(Component component) {
        return new Insets(1, 1, 1, 1);
    }
}
