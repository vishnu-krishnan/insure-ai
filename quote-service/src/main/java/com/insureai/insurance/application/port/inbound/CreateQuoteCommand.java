package com.insureai.insurance.application.port.inbound;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record CreateQuoteCommand(
    String customerName,
    String coverageType,
    int age,
    BigDecimal coverageAmount,
    Integer policyTermYears,
    String paymentFrequency,
    BigDecimal deductible,
    LocalDate startDate,
    String createdBy,
    Map<String, Object> details
) {
    public CreateQuoteCommand(
        String customerName,
        String coverageType,
        int age,
        BigDecimal coverageAmount,
        Integer policyTermYears,
        String paymentFrequency,
        BigDecimal deductible,
        LocalDate startDate,
        String createdBy
    ) {
        this(customerName, coverageType, age, coverageAmount, policyTermYears, paymentFrequency, deductible,
            startDate, createdBy, null);
    }
}
