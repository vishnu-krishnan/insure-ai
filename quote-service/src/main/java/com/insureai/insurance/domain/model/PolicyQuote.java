package com.insureai.insurance.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Objects;

public record PolicyQuote(
    String quoteId,
    String customerName,
    CoverageType coverageType,
    int age,
    BigDecimal coverageAmount,
    BigDecimal annualPremium,
    String customerId,
    String quoteNumber,
    String customerNumber,
    int policyTermYears,
    PaymentFrequency paymentFrequency,
    BigDecimal deductible,
    LocalDate startDate,
    BigDecimal taxRate,
    BigDecimal taxAmount,
    Instant createdAt,
    String createdBy
) {
    public static final int VALIDITY_DAYS = 30;

    public PolicyQuote {
        Objects.requireNonNull(quoteId, "quoteId is required");
        Objects.requireNonNull(customerName, "customerName is required");
        Objects.requireNonNull(coverageType, "coverageType is required");
        Objects.requireNonNull(coverageAmount, "coverageAmount is required");
        Objects.requireNonNull(annualPremium, "annualPremium is required");
        Objects.requireNonNull(paymentFrequency, "paymentFrequency is required");
        Objects.requireNonNull(deductible, "deductible is required");
        Objects.requireNonNull(startDate, "startDate is required");
        Objects.requireNonNull(taxRate, "taxRate is required");
        Objects.requireNonNull(taxAmount, "taxAmount is required");
        Objects.requireNonNull(createdAt, "createdAt is required");
    }

    public PolicyQuote withQuoteNumber(String assignedQuoteNumber) {
        return new PolicyQuote(
            quoteId, customerName, coverageType, age, coverageAmount, annualPremium, customerId,
            assignedQuoteNumber, customerNumber, policyTermYears, paymentFrequency, deductible, startDate,
            taxRate, taxAmount, createdAt, createdBy);
    }

    public BigDecimal monthlyPremium() {
        return annualPremium.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal totalAnnualPayable() {
        return annualPremium.add(taxAmount);
    }

    public BigDecimal installmentAmount() {
        return totalAnnualPayable().divide(BigDecimal.valueOf(paymentFrequency.periodsPerYear()), 2, RoundingMode.HALF_UP);
    }

    public LocalDate validUntil() {
        return LocalDate.ofInstant(createdAt, ZoneOffset.UTC).plusDays(VALIDITY_DAYS);
    }

    public QuoteStatus status(LocalDate today) {
        return today.isAfter(validUntil()) ? QuoteStatus.EXPIRED : QuoteStatus.ISSUED;
    }
}
