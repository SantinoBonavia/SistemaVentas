package com.sistemaventas.inventory.application;

public record ProductRegistrationCommand(String name, String price, String categoryName, String stock) {
}
