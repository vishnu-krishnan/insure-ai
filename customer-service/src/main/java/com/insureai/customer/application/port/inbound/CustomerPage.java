package com.insureai.customer.application.port.inbound;

import java.util.List;

import com.insureai.customer.domain.model.CustomerProfile;

public record CustomerPage(List<CustomerProfile> content, int page, int size, long totalElements) {
    public int totalPages() {
        return (int) ((totalElements + size - 1) / size);
    }
}
