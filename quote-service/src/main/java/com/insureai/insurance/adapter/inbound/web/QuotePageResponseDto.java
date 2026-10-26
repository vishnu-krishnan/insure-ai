package com.insureai.insurance.adapter.inbound.web;

import java.util.List;

import com.insureai.insurance.application.port.inbound.QuotePage;

public record QuotePageResponseDto(
    List<QuoteResponseDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static QuotePageResponseDto fromDomain(QuotePage quotePage) {
        return new QuotePageResponseDto(
            quotePage.content().stream().map(QuoteResponseDto::fromDomain).toList(),
            quotePage.page(),
            quotePage.size(),
            quotePage.totalElements(),
            quotePage.totalPages()
        );
    }
}
