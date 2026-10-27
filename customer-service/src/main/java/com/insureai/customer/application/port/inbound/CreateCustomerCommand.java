package com.insureai.customer.application.port.inbound;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.insureai.customer.domain.model.Address;
import com.insureai.customer.domain.model.Gender;
import com.insureai.customer.domain.model.MaritalStatus;
import com.insureai.customer.domain.model.Nominee;

public record CreateCustomerCommand(
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
}
