package com.insureai.insurance.adapter.inbound.web;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import com.insureai.insurance.application.port.inbound.CreateCustomerQuoteCommand;

public record CreateCustomerQuoteRequestDto(
    @NotBlank(message = "customerId is required")
    @Pattern(
        regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
        message = "customerId must be a valid UUID"
    )
    String customerId,

    @NotBlank(message = "coverageType is required")
    String coverageType,

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

    LocalDate startDate,

    Map<String, Object> details
) {
    public CreateCustomerQuoteCommand toCommand(String createdBy) {
        return new CreateCustomerQuoteCommand(
            customerId, coverageType.trim(), coverageAmount,
            policyTermYears, paymentFrequency, deductible, startDate, createdBy, details);
    }
}
