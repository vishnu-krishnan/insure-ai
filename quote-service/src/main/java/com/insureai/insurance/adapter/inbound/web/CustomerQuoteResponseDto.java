package com.insureai.insurance.adapter.inbound.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.insureai.insurance.domain.model.PolicyQuote;

public record CustomerQuoteResponseDto(
    String quoteId,
    String quoteNumber,
    String customerId,
    String customerNumber,
    String customerName,
    String coverageType,
    int age,
    BigDecimal coverageAmount,
    BigDecimal annualPremium,
    BigDecimal monthlyPremium,
    int policyTermYears,
    String paymentFrequency,
    BigDecimal deductible,
    LocalDate startDate,
    LocalDate validUntil,
    String status,
    BigDecimal taxRate,
    BigDecimal taxAmount,
    BigDecimal totalAnnualPayable,
    BigDecimal installmentAmount,
    Instant createdAt,
    String createdBy
) {
    public static CustomerQuoteResponseDto fromDomain(PolicyQuote quote, LocalDate today) {
        return new CustomerQuoteResponseDto(
            quote.quoteId(),
            quote.quoteNumber(),
            quote.customerId(),
            quote.customerNumber(),
            quote.customerName(),
            quote.coverageType().getCode(),
            quote.age(),
            quote.coverageAmount(),
            quote.annualPremium(),
            quote.monthlyPremium(),
            quote.policyTermYears(),
            quote.paymentFrequency().name(),
            quote.deductible(),
            quote.startDate(),
            quote.validUntil(),
            quote.status(today).name(),
            quote.taxRate(),
            quote.taxAmount(),
            quote.totalAnnualPayable(),
            quote.installmentAmount(),
            quote.createdAt(),
            quote.createdBy()
        );
    }
}
