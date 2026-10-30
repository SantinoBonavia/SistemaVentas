package com.sistemaventas;

import com.sistemaventas.inventory.application.ProductRegistrationCommand;
import com.sistemaventas.inventory.application.RegisterProduct;
import java.util.List;

/** Catálogo inicial de ferretería, solo para la primera ejecución. Precios finales con IVA (oct. 2026). */
final class DemoCatalog {
    private DemoCatalog() {
    }

    private record Item(String code, String name, String category, String price, String stock, String minStock,
                        String art) {
    }

    private static final String MANUALES = "Herramientas manuales";
    private static final String ELECTRICAS = "Herramientas eléctricas";
    private static final String FIJACIONES = "Fijaciones y adhesivos";
    private static final String ELECTRICIDAD = "Electricidad";
    private static final String PLOMERIA = "Plomería";
    private static final String PINTURERIA = "Pinturería";
    private static final String SEGURIDAD = "Seguridad";

    private static final List<Item> ITEMS = List.of(
            new Item("MAR-001", "Martillo carpintero 27 mm mango de fibra", MANUALES, "18.900", "14", "5", "martillo"),
            new Item("DES-010", "Juego de destornilladores x6", MANUALES, "15.400", "20", "6", "destornillador"),
            new Item("PIN-020", "Pinza universal 8\" aislada", MANUALES, "13.700", "3", "5", "pinza"),
            new Item("LLA-030", "Llave combinada 13 mm", MANUALES, "6.300", "25", "8", "llave"),
            new Item("LLA-031", "Llave francesa ajustable 10\"", MANUALES, "21.800", "7", "4", "llave_francesa"),
            new Item("SER-040", "Serrucho carpintero 20\"", MANUALES, "19.500", "6", "3", "serrucho"),
            new Item("CIN-050", "Cinta métrica 5 m", MANUALES, "8.900", "30", "10", "cinta_metrica"),
            new Item("NIV-060", "Nivel de aluminio 40 cm", MANUALES, "12.600", "9", "4", "nivel"),
            new Item("CAJ-070", "Caja de herramientas plástica 19\"", MANUALES, "32.500", "5", "2", "caja"),
            new Item("TAL-100", "Taladro percutor 13 mm 750 W", ELECTRICAS, "98.500", "4", "2", "taladro"),
            new Item("AMO-110", "Amoladora angular 115 mm 850 W", ELECTRICAS, "86.900", "0", "2", "amoladora"),
            new Item("TOR-200", "Tornillos autoperforantes 8 x 1\" caja x100", FIJACIONES, "5.900", "48", "15", "tornillos"),
            new Item("SIL-210", "Sellador de silicona transparente 280 g", FIJACIONES, "7.800", "18", "6", "silicona"),
            new Item("CIN-300", "Cinta aisladora 20 m", ELECTRICIDAD, "1.900", "60", "20", "cinta_aisladora"),
            new Item("FOC-310", "Lámpara LED 12 W luz fría", ELECTRICIDAD, "2.900", "40", "12", "foco"),
            new Item("CAB-320", "Cable unipolar 2,5 mm² rollo 100 m", ELECTRICIDAD, "74.000", "6", "2", "cable"),
            new Item("CAN-400", "Canilla monocomando para cocina", PLOMERIA, "64.900", "3", "2", "canilla"),
            new Item("PNT-500", "Látex interior blanco 4 L", PINTURERIA, "36.500", "12", "4", "pintura"),
            new Item("PNC-510", "Pincel Nº 20 cerda natural", PINTURERIA, "4.200", "22", "8", "pincel"),
            new Item("ROD-520", "Rodillo antigota 22 cm", PINTURERIA, "9.800", "2", "4", "rodillo"),
            new Item("GUA-600", "Guantes de trabajo de vaqueta", SEGURIDAD, "5.600", "35", "10", "guantes"),
            new Item("ANT-610", "Anteojos de seguridad claros", SEGURIDAD, "3.900", "16", "6", "anteojos"),
            new Item("CND-620", "Candado de bronce 40 mm", SEGURIDAD, "11.400", "10", "4", "candado"),
            new Item("ESC-700", "Escalera de aluminio 6 escalones", MANUALES, "115.000", "2", "1", "escalera")
    );

    static void load(RegisterProduct registerProduct) {
        for (Item item : ITEMS) {
            registerProduct.register(new ProductRegistrationCommand(item.name(), item.price(), item.category(),
                    item.stock(), item.code(), "21", item.minStock(), "art:" + item.art()));
        }
    }
}
