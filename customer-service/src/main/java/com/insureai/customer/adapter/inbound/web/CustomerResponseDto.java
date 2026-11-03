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
    Instant updatedAt,
    String idType,
    String idNumber,
    boolean profileComplete
) {
    /** For agents and other services: the ID type is shown but the number is masked. */
    public static CustomerResponseDto fromDomain(CustomerProfile customer) {
        return build(customer, false);
    }

    /** For the signed-in customer looking at their own profile: the whole ID number. */
    public static CustomerResponseDto forOwner(CustomerProfile customer) {
        return build(customer, true);
    }

    private static CustomerResponseDto build(CustomerProfile customer, boolean fullIdNumber) {
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
            customer.updatedAt(),
            customer.idDocument() == null ? null : customer.idDocument().type().name(),
            customer.idDocument() == null ? null
                : fullIdNumber ? customer.idDocument().number() : customer.idDocument().masked(),
            customer.isComplete()
        );
    }
}
