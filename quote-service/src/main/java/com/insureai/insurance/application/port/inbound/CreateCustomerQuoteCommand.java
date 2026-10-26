package com.insureai.insurance.application.port.inbound;

import java.math.BigDecimal;

public record CreateCustomerQuoteCommand(String customerId, String coverageType, BigDecimal coverageAmount) {
}
