package com.insureai.insurance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PaymentFrequency;
import com.insureai.insurance.domain.model.PolicyQuote;

public final class QuoteFixtures {
    public static final Instant CREATED_AT = Instant.parse("2026-10-26T10:15:30.123456Z");

    private QuoteFixtures() {
    }

    public static PolicyQuote quote(
        String id,
        String customerName,
        CoverageType coverageType,
        int age,
        String amount,
        String annualPremium
    ) {
        return quote(id, customerName, coverageType, age, amount, annualPremium, null, null, null);
    }

    public static PolicyQuote quote(
        String id,
        String customerName,
        CoverageType coverageType,
        int age,
        String amount,
        String annualPremium,
        String customerId,
        String quoteNumber,
        String customerNumber
    ) {
        BigDecimal premium = new BigDecimal(annualPremium);
        BigDecimal taxAmount = premium.multiply(new BigDecimal("0.18")).setScale(2, java.math.RoundingMode.HALF_UP);
        return new PolicyQuote(
            id,
            customerName,
            coverageType,
            age,
            new BigDecimal(amount),
            premium,
            customerId,
            quoteNumber,
            customerNumber,
            1,
            PaymentFrequency.YEARLY,
            new BigDecimal("0.00"),
            LocalDate.of(2026, 10, 26),
            new BigDecimal("0.1800"),
            taxAmount,
            CREATED_AT,
            null
        );
    }

    public static PolicyQuote withCreatedAt(PolicyQuote quote, Instant createdAt) {
        return new PolicyQuote(
            quote.quoteId(), quote.customerName(), quote.coverageType(), quote.age(), quote.coverageAmount(),
            quote.annualPremium(), quote.customerId(), quote.quoteNumber(), quote.customerNumber(),
            quote.policyTermYears(), quote.paymentFrequency(), quote.deductible(), quote.startDate(),
            quote.taxRate(), quote.taxAmount(), createdAt, quote.createdBy());
    }

    public static PolicyQuote withCreatedBy(PolicyQuote quote, String createdBy) {
        return new PolicyQuote(
            quote.quoteId(), quote.customerName(), quote.coverageType(), quote.age(), quote.coverageAmount(),
            quote.annualPremium(), quote.customerId(), quote.quoteNumber(), quote.customerNumber(),
            quote.policyTermYears(), quote.paymentFrequency(), quote.deductible(), quote.startDate(),
            quote.taxRate(), quote.taxAmount(), quote.createdAt(), createdBy);
    }
}
