package com.sistemaventas.inventory.application;

import java.math.BigDecimal;

public record ProductView(long id, String name, String categoryName, BigDecimal price, int stock) {
}
