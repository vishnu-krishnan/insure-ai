package com.insureai.customer.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.UUID;

public record CustomerProfile(
    String customerId,
    String customerNumber,
    String fullName,
    String email,
    String phone,
    LocalDate dateOfBirth,
    Gender gender,
    MaritalStatus maritalStatus,
    String occupation,
    BigDecimal annualIncome,
    Address address,
    Nominee nominee,
    Instant createdAt,
    Instant updatedAt
) {
    public CustomerProfile {
        if (customerId == null || fullName == null || email == null || phone == null || dateOfBirth == null) {
            throw new IllegalArgumentException("Customer profile fields are required");
        }
    }

    public static CustomerProfile create(
        String fullName,
        String email,
        String phone,
        LocalDate dateOfBirth,
        Gender gender,
        MaritalStatus maritalStatus,
        String occupation,
        BigDecimal annualIncome,
        Address address,
        Nominee nominee
    ) {
        return new CustomerProfile(
            UUID.randomUUID().toString(),
            null,
            fullName.trim(),
            email.trim().toLowerCase(Locale.ROOT),
            phone.trim(),
            dateOfBirth,
            gender,
            maritalStatus,
            occupation == null || occupation.isBlank() ? null : occupation.trim(),
            annualIncome,
            address,
            nominee,
            null,
            null
        );
    }
}
