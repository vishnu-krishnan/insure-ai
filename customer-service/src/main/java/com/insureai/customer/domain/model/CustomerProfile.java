package com.insureai.customer.domain.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public record CustomerProfile(
    String customerId,
    String fullName,
    String email,
    String phone,
    LocalDate dateOfBirth
) {
    public CustomerProfile {
        if (customerId == null || fullName == null || email == null || phone == null || dateOfBirth == null) {
            throw new IllegalArgumentException("Customer profile fields are required");
        }
    }

    public static CustomerProfile create(String fullName, String email, String phone, LocalDate dateOfBirth) {
        return new CustomerProfile(
            UUID.randomUUID().toString(),
            fullName.trim(),
            email.trim().toLowerCase(Locale.ROOT),
            phone.trim(),
            dateOfBirth
        );
    }
}
