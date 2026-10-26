package com.insureai.insurance.application.port.outbound;

import java.time.LocalDate;

public record CustomerProfileSnapshot(String customerId, String fullName, LocalDate dateOfBirth) {
}
