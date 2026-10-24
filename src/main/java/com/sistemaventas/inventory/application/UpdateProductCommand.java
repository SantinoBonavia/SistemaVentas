package com.sistemaventas.inventory.application;

public record UpdateProductCommand(long id, String name, String price, String categoryName, String stock) {
}
