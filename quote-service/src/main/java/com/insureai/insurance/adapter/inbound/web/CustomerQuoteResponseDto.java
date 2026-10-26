package com.insureai.insurance.adapter.inbound.web;

import java.math.BigDecimal;

import com.insureai.insurance.domain.model.PolicyQuote;

public record CustomerQuoteResponseDto(
    String quoteId,
    String customerId,
    String customerName,
    String coverageType,
    int age,
    BigDecimal coverageAmount,
    BigDecimal annualPremium,
    BigDecimal monthlyPremium
) {
    public static CustomerQuoteResponseDto fromDomain(PolicyQuote quote) {
        return new CustomerQuoteResponseDto(
            quote.quoteId(),
            quote.customerId(),
            quote.customerName(),
            quote.coverageType().getCode(),
            quote.age(),
            quote.coverageAmount(),
            quote.annualPremium(),
            quote.monthlyPremium()
        );
    }
}
