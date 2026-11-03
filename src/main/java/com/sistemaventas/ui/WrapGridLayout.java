package com.sistemaventas.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/** Grilla que acomoda tantas columnas como entren, con celdas de ancho flexible y alto fijo. */
final class WrapGridLayout implements LayoutManager {
    private final int minCellWidth;
    private final int cellHeight;
    private final int gap;

    WrapGridLayout(int minCellWidth, int cellHeight, int gap) {
        this.minCellWidth = minCellWidth;
        this.cellHeight = cellHeight;
        this.gap = gap;
    }

    @Override
    public void addLayoutComponent(String name, Component component) {
    }

    @Override
    public void removeLayoutComponent(Component component) {
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        int width = availableWidth(parent);
        int columns = columns(width);
        int rows = (visibleCount(parent) + columns - 1) / columns;
        Insets insets = parent.getInsets();
        int height = rows == 0 ? 0 : rows * cellHeight + (rows - 1) * gap;
        return new Dimension(width + insets.left + insets.right, height + insets.top + insets.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return new Dimension(minCellWidth, cellHeight);
    }

    @Override
    public void layoutContainer(Container parent) {
        Insets insets = parent.getInsets();
        int width = parent.getWidth() - insets.left - insets.right;
        int columns = columns(width);
        int cellWidth = (width - (columns - 1) * gap) / columns;
        int index = 0;
        for (Component component : parent.getComponents()) {
            if (!component.isVisible()) continue;
            int column = index % columns;
            int row = index / columns;
            component.setBounds(insets.left + column * (cellWidth + gap), insets.top + row * (cellHeight + gap),
                    cellWidth, cellHeight);
            index++;
        }
    }

    private int columns(int width) {
        return Math.max(1, (width + gap) / (minCellWidth + gap));
    }

    private int availableWidth(Container parent) {
        Container viewport = parent.getParent();
        int width = viewport instanceof javax.swing.JViewport ? viewport.getWidth() : parent.getWidth();
        Insets insets = parent.getInsets();
        return Math.max(minCellWidth, width - insets.left - insets.right);
    }

    private static int visibleCount(Container parent) {
        int count = 0;
        for (Component component : parent.getComponents()) if (component.isVisible()) count++;
        return count;
    }
}
