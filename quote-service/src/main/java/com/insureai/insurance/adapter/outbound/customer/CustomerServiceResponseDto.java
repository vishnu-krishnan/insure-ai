package com.insureai.insurance.adapter.outbound.customer;

import java.time.LocalDate;

public record CustomerServiceResponseDto(
    String customerId,
    String customerNumber,
    String fullName,
    String email,
    String phone,
    LocalDate dateOfBirth
) {
}
