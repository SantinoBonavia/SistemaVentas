package com.sistemaventas.ui;

import com.sistemaventas.inventory.application.ProductView;
import com.sistemaventas.sales.application.Pesos;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

/** Tarjeta de producto del mostrador: foto, nombre, precio final y estado de stock. */
final class ProductCard extends JPanel {
    static final int HEIGHT = 286;
    private final ProductView product;
    private final int inCart;
    private boolean hover;

    ProductCard(ProductView product, int inCart, Consumer<ProductView> onPick) {
        this.product = product;
        this.inCart = inCart;
        setOpaque(false);
        setName("card-" + product.id());
        setFocusable(true);
        setToolTipText(product.name() + " — " + Pesos.format(product.price()) + " (IVA incluido)");
        getAccessibleContext().setAccessibleName(product.name());
        boolean available = product.stock() > 0;
        setCursor(Cursor.getPredefinedCursor(available ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent event) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent event) {
                hover = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (available) onPick.accept(product);
            }
        });
        registerKeyboardAction(event -> {
            if (available) onPick.accept(product);
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), WHEN_FOCUSED);
        addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent event) {
                repaint();
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent event) {
                repaint();
            }
        });
    }

    ProductView product() {
        return product;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        int w = getWidth();
        int h = getHeight();
        boolean available = product.stock() > 0;
        boolean highlighted = (hover || isFocusOwner()) && available;

        if (highlighted) {
            g.setColor(new Color(232, 89, 12, 30));
            g.fill(new RoundRectangle2D.Double(1, 3, w - 2, h - 2, 18, 18));
        }
        RoundRectangle2D card = new RoundRectangle2D.Double(0.5, 0.5, w - 2, h - 3, 16, 16);
        g.setColor(Theme.SURFACE);
        g.fill(card);
        g.setColor(highlighted ? Theme.ACCENT : inCart > 0 ? new Color(0xF6B98F) : Theme.BORDER);
        g.setStroke(new BasicStroke(highlighted || inCart > 0 ? 2f : 1f));
        g.draw(card);

        int pad = 10;
        int imageBox = 150;
        g.setColor(Theme.tint(product.categoryName()));
        g.fill(new RoundRectangle2D.Double(pad, pad, w - pad * 2 - 1, imageBox, 12, 12));
        int art = 128;
        if (ProductImages.isPhoto(product.image())) {
            g.setClip(new RoundRectangle2D.Double(pad, pad, w - pad * 2 - 1, imageBox, 12, 12));
            int size = Math.max(w - pad * 2, imageBox);
            g.drawImage(ProductImages.image(product.image(), size), pad + (w - pad * 2 - size) / 2, pad + (imageBox - size) / 2, null);
            g.setClip(null);
        } else {
            g.drawImage(ProductImages.image(product.image(), art), (w - art) / 2, pad + (imageBox - art) / 2, null);
        }

        paintStockBadge(g, w - pad - 8, pad + 8);
        if (inCart > 0) {
            String text = Integer.toString(inCart);
            g.setFont(Theme.font(Font.BOLD, 13f));
            int size = Math.max(26, g.getFontMetrics().stringWidth(text) + 14);
            g.setColor(Theme.ACCENT);
            g.fillRoundRect(pad + 8, pad + 8, size, 26, 26, 26);
            g.setColor(Color.WHITE);
            g.drawString(text, pad + 8 + (size - g.getFontMetrics().stringWidth(text)) / 2, pad + 26);
        }

        int y = pad + imageBox + 20;
        g.setFont(Theme.font(Font.PLAIN, 11.5f));
        g.setColor(Theme.MUTED);
        String meta = (product.code().isEmpty() ? "" : product.code() + " · ") + product.categoryName();
        g.drawString(ellipsize(meta, g.getFontMetrics(), w - pad * 2 - 4), pad + 2, y);

        g.setFont(Theme.font(Font.BOLD, 13.5f));
        g.setColor(available ? Theme.TEXT : Theme.MUTED);
        FontMetrics nameMetrics = g.getFontMetrics();
        List<String> lines = wrap(product.name(), nameMetrics, w - pad * 2 - 4, 2);
        for (String line : lines) {
            y += nameMetrics.getHeight() - 1;
            g.drawString(line, pad + 2, y);
        }

        g.setFont(Theme.font(Font.BOLD, 20f));
        g.setColor(available ? Theme.TEXT : Theme.MUTED);
        int priceY = h - 20;
        String price = Pesos.compact(product.price());
        g.drawString(price, pad + 2, priceY);
        int priceWidth = g.getFontMetrics().stringWidth(price);
        g.setFont(Theme.font(Font.PLAIN, 11f));
        g.setColor(Theme.MUTED);
        g.drawString("IVA incl.", pad + 8 + priceWidth, priceY);

        if (!available) {
            g.setColor(new Color(255, 255, 255, 120));
            g.fill(card);
        }
        g.dispose();
    }

    private void paintStockBadge(Graphics2D g, int right, int top) {
        String text;
        Color background;
        Color foreground;
        switch (product.stockStatus()) {
            case SIN_STOCK -> {
                text = "Sin stock";
                background = Theme.DANGER;
                foreground = Color.WHITE;
            }
            case BAJO -> {
                text = "Quedan " + product.stock();
                background = Theme.WARNING_SOFT;
                foreground = Theme.WARNING;
            }
            default -> {
                text = "Stock " + product.stock();
                background = Theme.SUCCESS_SOFT;
                foreground = Theme.SUCCESS;
            }
        }
        g.setFont(Theme.font(Font.BOLD, 11.5f));
        int width = g.getFontMetrics().stringWidth(text) + 16;
        g.setColor(background);
        g.fillRoundRect(right - width, top, width, 22, 22, 22);
        g.setColor(foreground);
        g.drawString(text, right - width + 8, top + 15);
    }

    static String ellipsize(String text, FontMetrics metrics, int width) {
        if (metrics.stringWidth(text) <= width) return text;
        String result = text;
        while (!result.isEmpty() && metrics.stringWidth(result + "…") > width) {
            result = result.substring(0, result.length() - 1);
        }
        return result.stripTrailing() + "…";
    }

    static List<String> wrap(String text, FontMetrics metrics, int width, int maxLines) {
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] words = text.split("\\s+");
        for (int i = 0; i < words.length; i++) {
            String candidate = current.isEmpty() ? words[i] : current + " " + words[i];
            if (metrics.stringWidth(candidate) <= width || current.isEmpty()) {
                current = new StringBuilder(candidate);
            } else {
                lines.add(current.toString());
                current = new StringBuilder(words[i]);
                if (lines.size() == maxLines - 1) {
                    String rest = String.join(" ", java.util.Arrays.copyOfRange(words, i, words.length));
                    lines.add(ellipsize(rest, metrics, width));
                    return lines;
                }
            }
        }
        if (!current.isEmpty()) lines.add(ellipsize(current.toString(), metrics, width));
        return lines;
    }
}
