package com.sistemaventas.inventory.application;

public sealed interface RegistrationResult permits RegistrationResult.Created, RegistrationResult.ValidationFailed,
        RegistrationResult.ConfirmationRequired, RegistrationResult.Failure {

    record Created(long productId, String message) implements RegistrationResult {
    }

    record ValidationFailed(String field, String message) implements RegistrationResult {
    }

    record ConfirmationRequired(DuplicateConfirmation confirmation) implements RegistrationResult {
    }

    record Failure(String message) implements RegistrationResult {
    }
}
