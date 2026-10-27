package com.insureai.insurance.adapter.inbound.web;

import java.time.LocalDate;
import java.util.List;

import com.insureai.insurance.application.port.inbound.QuotePage;

public record QuotePageResponseDto(
    List<QuoteResponseDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static QuotePageResponseDto fromDomain(QuotePage quotePage, LocalDate today) {
        return new QuotePageResponseDto(
            quotePage.content().stream().map(quote -> QuoteResponseDto.fromDomain(quote, today)).toList(),
            quotePage.page(),
            quotePage.size(),
            quotePage.totalElements(),
            quotePage.totalPages()
        );
    }
}
