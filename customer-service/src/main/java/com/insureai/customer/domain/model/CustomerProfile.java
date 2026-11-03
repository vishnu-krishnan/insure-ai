package com.insureai.customer.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;

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
    Instant updatedAt,
    IdDocument idDocument
) {
    public CustomerProfile(
        String customerId, String customerNumber, String fullName, String email, String phone,
        LocalDate dateOfBirth, Gender gender, MaritalStatus maritalStatus, String occupation,
        BigDecimal annualIncome, Address address, Nominee nominee, Instant createdAt, Instant updatedAt
    ) {
        this(customerId, customerNumber, fullName, email, phone, dateOfBirth, gender, maritalStatus, occupation,
            annualIncome, address, nominee, createdAt, updatedAt, null);
    }

    public CustomerProfile {
        if (fullName == null || email == null || phone == null || dateOfBirth == null) {
            throw new IllegalArgumentException("Customer profile fields are required");
        }
    }

    /** Every detail needed to price cover is on file; quotes for the customer's own account wait for this. */
    public boolean isComplete() {
        return gender != null && maritalStatus != null && occupation != null && !occupation.isBlank()
            && annualIncome != null && address != null && nominee != null && idDocument != null;
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
        return create(fullName, email, phone, dateOfBirth, gender, maritalStatus, occupation, annualIncome, address,
            nominee, null);
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
        Nominee nominee,
        IdDocument idDocument
    ) {
        return new CustomerProfile(
            null,
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
            null,
            idDocument
        );
    }
}
