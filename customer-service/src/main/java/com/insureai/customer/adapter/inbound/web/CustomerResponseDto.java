package com.insureai.customer.adapter.inbound.web;

import java.time.LocalDate;

import com.insureai.customer.domain.model.CustomerProfile;

public record CustomerResponseDto(
    String customerId,
    String fullName,
    String email,
    String phone,
    LocalDate dateOfBirth
) {
    public static CustomerResponseDto fromDomain(CustomerProfile customer) {
        return new CustomerResponseDto(
            customer.customerId(),
            customer.fullName(),
            customer.email(),
            customer.phone(),
            customer.dateOfBirth()
        );
    }
}
