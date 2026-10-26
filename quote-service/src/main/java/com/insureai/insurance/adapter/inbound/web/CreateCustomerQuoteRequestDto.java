package com.insureai.insurance.adapter.inbound.web;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
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
    BigDecimal coverageAmount
) {
    public CreateCustomerQuoteCommand toCommand() {
        return new CreateCustomerQuoteCommand(customerId, coverageType.trim(), coverageAmount);
    }
}
