package com.insureai.insurance.adapter.inbound.web;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.insureai.insurance.application.port.inbound.CreateQuoteCommand;

public record QuoteRequestDto(
    @NotBlank(message = "customerName is required")
    @Size(min = 2, max = 80, message = "customerName must be between 2 and 80 characters")
    @Pattern(regexp = "^[A-Za-z .'-]+$", message = "customerName contains invalid characters")
    String customerName,

    @NotBlank(message = "coverageType is required")
    String coverageType,

    @NotNull(message = "age is required")
    @Min(value = 18, message = "age must be at least 18")
    @Max(value = 75, message = "age must be at most 75")
    Integer age,

    @NotNull(message = "coverageAmount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "coverageAmount must be greater than zero")
    BigDecimal coverageAmount,

    @Min(value = 1, message = "policyTermYears must be at least 1")
    @Max(value = 40, message = "policyTermYears must be at most 40")
    Integer policyTermYears,

    @Pattern(
        regexp = "(?i)^(MONTHLY|QUARTERLY|YEARLY)$",
        message = "paymentFrequency must be one of MONTHLY, QUARTERLY, YEARLY"
    )
    String paymentFrequency,

    @DecimalMin(value = "0", inclusive = true, message = "deductible must be zero or greater")
    @Digits(integer = 12, fraction = 2, message = "deductible must have at most 12 integer digits and 2 decimals")
    BigDecimal deductible,

    LocalDate startDate
) {
    public CreateQuoteCommand toCommand(String createdBy) {
        return new CreateQuoteCommand(
            customerName.trim(), coverageType.trim(), age, coverageAmount,
            policyTermYears, paymentFrequency, deductible, startDate, createdBy);
    }
}
