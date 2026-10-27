package com.insureai.customer.domain.model;

import java.util.Locale;

public record Address(
    String line1,
    String line2,
    String city,
    String state,
    String postalCode,
    String country
) {
    public Address {
        if (line1 == null || city == null || state == null || postalCode == null || country == null) {
            throw new IllegalArgumentException("Address line1, city, state, postalCode and country are required");
        }
        line1 = line1.trim();
        line2 = line2 == null || line2.isBlank() ? null : line2.trim();
        city = city.trim();
        state = state.trim();
        postalCode = postalCode.trim();
        country = country.trim().toUpperCase(Locale.ROOT);
    }
}
