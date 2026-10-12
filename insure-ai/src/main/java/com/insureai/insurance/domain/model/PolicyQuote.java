package com.insureai.insurance.domain.model;

import java.math.BigDecimal;
import java.util.Objects;

public record PolicyQuote(
    String quoteId,
    String customerName,
    CoverageType coverageType,
    int age,
    BigDecimal coverageAmount,
    BigDecimal annualPremium
) {
    public PolicyQuote {
        Objects.requireNonNull(quoteId, "quoteId is required");
        Objects.requireNonNull(customerName, "customerName is required");
        Objects.requireNonNull(coverageType, "coverageType is required");
        Objects.requireNonNull(coverageAmount, "coverageAmount is required");
        Objects.requireNonNull(annualPremium, "annualPremium is required");
    }

    public BigDecimal monthlyPremium() {
        return annualPremium.divide(BigDecimal.valueOf(12), 2, java.math.RoundingMode.HALF_UP);
    }
}
