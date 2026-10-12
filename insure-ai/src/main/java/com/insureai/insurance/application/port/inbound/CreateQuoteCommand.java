package com.insureai.insurance.application.port.inbound;

import java.math.BigDecimal;

public record CreateQuoteCommand(
    String customerName,
    String coverageType,
    int age,
    BigDecimal coverageAmount
) {
}
