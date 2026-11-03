package com.sistemaventas.inventory.application;

/** Datos tal como se escriben en el formulario; {@code price} es el precio final con IVA. */
public record ProductRegistrationCommand(String name, String price, String categoryName, String stock, String code,
                                         String ivaPercent, String minStock, String image) {

    public ProductRegistrationCommand(String name, String price, String categoryName, String stock) {
        this(name, price, categoryName, stock, "", "", "", "");
    }
}
