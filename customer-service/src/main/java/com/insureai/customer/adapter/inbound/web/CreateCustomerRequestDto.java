package com.insureai.customer.adapter.inbound.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.insureai.customer.application.port.inbound.CreateCustomerCommand;
import com.insureai.customer.domain.model.Gender;
import com.insureai.customer.domain.model.MaritalStatus;

public record CreateCustomerRequestDto(
    @NotBlank(message = "fullName is required")
    @Size(min = 2, max = 80, message = "fullName must be between 2 and 80 characters")
    String fullName,

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Size(max = 254, message = "email must not exceed 254 characters")
    String email,

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$", message = "phone must be in E.164 format")
    String phone,

    @NotNull(message = "dateOfBirth is required")
    @Past(message = "dateOfBirth must be in the past")
    LocalDate dateOfBirth,

    @Pattern(regexp = "^(MALE|FEMALE|OTHER|UNSPECIFIED)$", message = "gender must be one of MALE, FEMALE, OTHER, UNSPECIFIED")
    String gender,

    @Pattern(regexp = "^(SINGLE|MARRIED|DIVORCED|WIDOWED)$", message = "maritalStatus must be one of SINGLE, MARRIED, DIVORCED, WIDOWED")
    String maritalStatus,

    @Pattern(regexp = "^\\s*(\\S(.{0,78}\\S)?)?\\s*$", message = "occupation must not exceed 80 characters")
    String occupation,

    @DecimalMin(value = "0", message = "annualIncome must not be negative")
    @Digits(integer = 12, fraction = 2, message = "annualIncome must have at most 12 integer digits and 2 decimals")
    BigDecimal annualIncome,

    @Valid
    AddressDto address,

    @Valid
    NomineeDto nominee
) {
    public CreateCustomerCommand toCommand() {
        return new CreateCustomerCommand(
            fullName.trim(),
            email.trim(),
            phone.trim(),
            dateOfBirth,
            gender == null ? null : Gender.valueOf(gender),
            maritalStatus == null ? null : MaritalStatus.valueOf(maritalStatus),
            occupation == null || occupation.isBlank() ? null : occupation.trim(),
            annualIncome,
            address == null ? null : address.toDomain(),
            nominee == null ? null : nominee.toDomain()
        );
    }
}
