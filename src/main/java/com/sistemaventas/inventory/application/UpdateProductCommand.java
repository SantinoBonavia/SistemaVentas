package com.sistemaventas.inventory.application;

public record UpdateProductCommand(long id, String name, String price, String categoryName, String stock, String code,
                                   String ivaPercent, String minStock, String image) {

    public UpdateProductCommand(long id, String name, String price, String categoryName, String stock) {
        this(id, name, price, categoryName, stock, "", "", "", "");
    }
}
