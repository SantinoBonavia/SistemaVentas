package com.sistemaventas.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.swing.JButton;
import org.junit.jupiter.api.Test;

class VintageThemeTest {

    @Test
    void suppliesLegibleFontsContrastAndVisibleFocusWithoutUsingColorAsTheOnlyCue() {
        JButton button = new JButton("Save settings");
        VintageTheme.applyTo(button);

        assertTrue(VintageTheme.contrastRatio(VintageTheme.FOREGROUND, VintageTheme.BACKGROUND) >= 4.5);
        assertTrue(button.getFont().getSize() >= 14);
        assertNotNull(button.getBorder());
        assertTrue(button.isFocusPainted());
        assertNotNull(button.getAccessibleContext().getAccessibleName());
    }
}
