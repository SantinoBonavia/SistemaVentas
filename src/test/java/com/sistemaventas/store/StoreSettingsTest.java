package com.sistemaventas.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.sistemaventas.tax.CondicionIva;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class StoreSettingsTest {

    @Test
    void savesANormalizedCuitAndNotifiesListeners() {
        StoreSettings settings = new StoreSettings();
        AtomicInteger saves = new AtomicInteger();
        settings.onChange(saves::incrementAndGet);

        StoreSettings.Result result = settings.save("Ferretería Don Pepe", "Don Pepe SRL", "30712345671",
                "San Martín 100", "341 555-0000", CondicionIva.RESPONSABLE_INSCRIPTO, "3");

        StoreProfile saved = assertInstanceOf(StoreSettings.Result.Saved.class, result).profile();
        assertEquals("30-71234567-1", saved.cuit());
        assertEquals(3, saved.pointOfSale());
        assertEquals(saved, settings.profile());
        assertEquals(1, saves.get());
    }

    @Test
    void rejectsInvalidDataWithoutChangingTheProfile() {
        StoreSettings settings = new StoreSettings();

        assertEquals("El CUIT no es válido. Revisá los 11 dígitos.", ((StoreSettings.Result.Invalid) settings.save(
                "X", "", "30-71234567-2", "", "", CondicionIva.RESPONSABLE_INSCRIPTO, "1")).message());
        assertInstanceOf(StoreSettings.Result.Invalid.class,
                settings.save(" ", "", "", "", "", CondicionIva.RESPONSABLE_INSCRIPTO, "1"));
        assertInstanceOf(StoreSettings.Result.Invalid.class,
                settings.save("X", "", "", "", "", CondicionIva.RESPONSABLE_INSCRIPTO, "0"));
        assertEquals(StoreProfile.DEFAULT, settings.profile());
    }
}
