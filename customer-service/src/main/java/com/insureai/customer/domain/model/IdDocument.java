package com.insureai.customer.domain.model;

import java.util.Locale;

public record IdDocument(IdType type, String number) {
    public IdDocument {
        if (type == null || number == null) {
            throw new IllegalArgumentException("ID type and number are required");
        }
        number = normalise(number);
    }

    /** Upper case with spaces and hyphens removed, so "1234 5678 9012" and "123456789012" are the same ID. */
    public static String normalise(String number) {
        return number.trim().replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    /** Last four characters only, for screens that must not show the whole number. */
    public String masked() {
        int visible = Math.min(4, number.length());
        return "X".repeat(number.length() - visible) + number.substring(number.length() - visible);
    }
}
