package com.sistemaventas.ui;

import java.awt.BorderLayout;
import java.util.Objects;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/** Headless-safe shell. The application composition layer may place {@link #component()} in a JFrame later. */
public final class MainFrame {
    private final JPanel root = new JPanel(new BorderLayout(8, 8));
    private final JTabbedPane tabs = new JTabbedPane();

    public MainFrame(JComponent loadingComponent, JComponent productsComponent, Runnable openSettings) {
        JButton settings = new JButton("Settings");
        settings.setName("Settings");
        settings.addActionListener(event -> Objects.requireNonNull(openSettings).run());
        VintageTheme.applyTo(settings);
        tabs.addTab("Load Product", Objects.requireNonNull(loadingComponent));
        tabs.addTab("Products", Objects.requireNonNull(productsComponent));
        VintageTheme.applyTo(tabs);
        root.setBackground(VintageTheme.BACKGROUND);
        root.setFocusable(true);
        root.add(settings, BorderLayout.NORTH);
        root.add(tabs, BorderLayout.CENTER);
    }

    public JPanel component() {
        return root;
    }

    public JTabbedPane tabs() {
        return tabs;
    }
}
