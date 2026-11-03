package com.sistemaventas.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/** FlowLayout que pasa a la línea siguiente y reporta el alto real, para filas de chips. */
final class WrapFlowLayout extends FlowLayout {
    WrapFlowLayout(int hgap, int vgap) {
        super(LEADING, hgap, vgap);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
        Dimension minimum = layoutSize(target, false);
        minimum.width -= getHgap() + 1;
        return minimum;
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int targetWidth = target.getSize().width;
            Container container = target;
            while (container.getSize().width == 0 && container.getParent() != null) container = container.getParent();
            targetWidth = container.getSize().width;
            if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;
            Insets insets = target.getInsets();
            int maxWidth = targetWidth - (insets.left + insets.right + getHgap() * 2);
            Dimension dimension = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;
            for (Component component : target.getComponents()) {
                if (!component.isVisible()) continue;
                Dimension size = preferred ? component.getPreferredSize() : component.getMinimumSize();
                if (rowWidth + size.width > maxWidth) {
                    addRow(dimension, rowWidth, rowHeight);
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth != 0) rowWidth += getHgap();
                rowWidth += size.width;
                rowHeight = Math.max(rowHeight, size.height);
            }
            addRow(dimension, rowWidth, rowHeight);
            dimension.width += insets.left + insets.right + getHgap() * 2;
            dimension.height += insets.top + insets.bottom + getVgap() * 2;
            return dimension;
        }
    }

    private void addRow(Dimension dimension, int rowWidth, int rowHeight) {
        dimension.width = Math.max(dimension.width, rowWidth);
        if (dimension.height > 0) dimension.height += getVgap();
        dimension.height += rowHeight;
    }
}
