package com.insureai.customer.adapter.inbound.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.insureai.customer.application.port.inbound.CreateCustomerCommand;
import com.insureai.customer.domain.exception.CustomerValidationException;
import com.insureai.customer.domain.model.Gender;
import com.insureai.customer.domain.model.IdDocument;
import com.insureai.customer.domain.model.IdType;
import com.insureai.customer.domain.model.MaritalStatus;

public record MyProfileRequestDto(
    @NotBlank(message = "fullName is required")
    @Size(min = 2, max = 80, message = "fullName must be between 2 and 80 characters")
    @Pattern(regexp = "^\\s*\\p{L}[\\p{L}\\p{M} .'-]*$", message = "fullName may contain only letters, spaces, apostrophes, hyphens and periods")
    String fullName,

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
    NomineeDto nominee,

    @Pattern(regexp = "^(AADHAAR|PAN|PASSPORT|DRIVING_LICENCE|VOTER_ID)$", message = "idType must be one of AADHAAR, PAN, PASSPORT, DRIVING_LICENCE, VOTER_ID")
    String idType,

    @Size(max = 30, message = "idNumber must not exceed 30 characters")
    String idNumber
) {
    private IdDocument idDocument() {
        boolean hasType = idType != null && !idType.isBlank();
        boolean hasNumber = idNumber != null && !idNumber.isBlank();
        if (hasType != hasNumber) {
            throw new CustomerValidationException("idType and idNumber must be given together");
        }
        return hasType ? new IdDocument(IdType.valueOf(idType), idNumber) : null;
    }

    public CreateCustomerCommand toCommand() {
        return new CreateCustomerCommand(
            fullName.trim(),
            null,
            phone.trim(),
            dateOfBirth,
            gender == null ? null : Gender.valueOf(gender),
            maritalStatus == null ? null : MaritalStatus.valueOf(maritalStatus),
            occupation == null || occupation.isBlank() ? null : occupation.trim(),
            annualIncome,
            address == null ? null : address.toDomain(),
            nominee == null ? null : nominee.toDomain(),
            idDocument()
        );
    }
}
