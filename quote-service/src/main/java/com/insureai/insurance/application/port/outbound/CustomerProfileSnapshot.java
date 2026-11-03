package com.insureai.insurance.application.port.outbound;

import java.time.LocalDate;

public record CustomerProfileSnapshot(
    String customerId, String fullName, LocalDate dateOfBirth, String customerNumber, boolean profileComplete
) {
    public CustomerProfileSnapshot(String customerId, String fullName, LocalDate dateOfBirth, String customerNumber) {
        this(customerId, fullName, dateOfBirth, customerNumber, true);
    }
}
