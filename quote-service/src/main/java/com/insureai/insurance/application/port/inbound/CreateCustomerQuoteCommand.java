package com.insureai.insurance.application.port.inbound;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record CreateCustomerQuoteCommand(
    String customerId,
    String coverageType,
    BigDecimal coverageAmount,
    Integer policyTermYears,
    String paymentFrequency,
    BigDecimal deductible,
    LocalDate startDate,
    String createdBy,
    Map<String, Object> details
) {
    public CreateCustomerQuoteCommand(
        String customerId,
        String coverageType,
        BigDecimal coverageAmount,
        Integer policyTermYears,
        String paymentFrequency,
        BigDecimal deductible,
        LocalDate startDate,
        String createdBy
    ) {
        this(customerId, coverageType, coverageAmount, policyTermYears, paymentFrequency, deductible, startDate,
            createdBy, null);
    }
}
