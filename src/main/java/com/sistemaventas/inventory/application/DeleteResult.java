package com.sistemaventas.inventory.application;

public sealed interface DeleteResult {
    record Deleted(long id) implements DeleteResult {}
    record ConfirmationRequired(ProductDeletionConfirmation confirmation) implements DeleteResult {}
    record Failure(String message) implements DeleteResult {}
}
