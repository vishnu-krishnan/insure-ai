package com.insureai.insurance.application.port.inbound;

import java.util.List;

import com.insureai.insurance.domain.model.PolicyQuote;

public record QuotePage(List<PolicyQuote> content, int page, int size, long totalElements) {
    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
