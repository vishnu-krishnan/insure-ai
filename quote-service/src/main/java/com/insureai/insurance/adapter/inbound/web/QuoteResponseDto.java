package com.insureai.insurance.adapter.inbound.web;

import java.math.BigDecimal;

import com.insureai.insurance.domain.model.PolicyQuote;

public record QuoteResponseDto(
    String quoteId,
    String customerName,
    String coverageType,
    int age,
    BigDecimal coverageAmount,
    BigDecimal annualPremium,
    BigDecimal monthlyPremium
) {
    public static QuoteResponseDto fromDomain(PolicyQuote quote) {
        return new QuoteResponseDto(
            quote.quoteId(),
            quote.customerName(),
            quote.coverageType().getCode(),
            quote.age(),
            quote.coverageAmount(),
            quote.annualPremium(),
            quote.monthlyPremium()
        );
    }
}
