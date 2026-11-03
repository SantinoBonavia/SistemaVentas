package com.sistemaventas.tax;

import java.util.Optional;

/** CUIT/CUIL argentino: 11 dígitos con dígito verificador módulo 11. */
public final class Cuit {
    private static final int[] WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private Cuit() {
    }

    /** Devuelve el CUIT con formato XX-XXXXXXXX-X si es válido; acepta guiones, puntos o espacios. */
    public static Optional<String> normalize(String input) {
        String digits = input == null ? "" : input.replaceAll("[\\s.-]", "");
        if (!digits.matches("\\d{11}") || !hasValidCheckDigit(digits)) {
            return Optional.empty();
        }
        return Optional.of(digits.substring(0, 2) + "-" + digits.substring(2, 10) + "-" + digits.substring(10));
    }

    public static boolean isValid(String input) {
        return normalize(input).isPresent();
    }

    private static boolean hasValidCheckDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < WEIGHTS.length; i++) {
            sum += (digits.charAt(i) - '0') * WEIGHTS[i];
        }
        int check = 11 - (sum % 11);
        if (check == 11) {
            check = 0;
        } else if (check == 10) {
            return false;
        }
        return check == digits.charAt(10) - '0';
    }
}
