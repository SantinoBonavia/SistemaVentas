package com.sistemaventas.inventory.application;

public sealed interface UpdateResult {
    record Updated(long id) implements UpdateResult {}
    record ConfirmationRequired(ProductUpdateConfirmation confirmation) implements UpdateResult {}
    record ValidationFailed(String field, String message) implements UpdateResult {}
    record Failure(String message) implements UpdateResult {}
}
