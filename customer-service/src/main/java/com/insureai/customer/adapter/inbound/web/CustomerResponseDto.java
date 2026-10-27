package com.insureai.customer.adapter.inbound.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.insureai.customer.domain.model.CustomerProfile;

public record CustomerResponseDto(
    String customerId,
    String customerNumber,
    String fullName,
    String email,
    String phone,
    LocalDate dateOfBirth,
    String gender,
    String maritalStatus,
    String occupation,
    BigDecimal annualIncome,
    AddressDto address,
    NomineeDto nominee,
    Instant createdAt,
    Instant updatedAt
) {
    public static CustomerResponseDto fromDomain(CustomerProfile customer) {
        return new CustomerResponseDto(
            customer.customerId(),
            customer.customerNumber(),
            customer.fullName(),
            customer.email(),
            customer.phone(),
            customer.dateOfBirth(),
            customer.gender() == null ? null : customer.gender().name(),
            customer.maritalStatus() == null ? null : customer.maritalStatus().name(),
            customer.occupation(),
            customer.annualIncome(),
            AddressDto.fromDomain(customer.address()),
            NomineeDto.fromDomain(customer.nominee()),
            customer.createdAt(),
            customer.updatedAt()
        );
    }
}
