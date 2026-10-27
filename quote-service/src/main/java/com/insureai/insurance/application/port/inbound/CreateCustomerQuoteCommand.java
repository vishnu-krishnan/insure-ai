package com.insureai.insurance.application.port.inbound;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateCustomerQuoteCommand(
    String customerId,
    String coverageType,
    BigDecimal coverageAmount,
    Integer policyTermYears,
    String paymentFrequency,
    BigDecimal deductible,
    LocalDate startDate,
    String createdBy
) {
}
