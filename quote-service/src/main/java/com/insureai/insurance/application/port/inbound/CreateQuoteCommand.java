package com.insureai.insurance.application.port.inbound;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateQuoteCommand(
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
}
