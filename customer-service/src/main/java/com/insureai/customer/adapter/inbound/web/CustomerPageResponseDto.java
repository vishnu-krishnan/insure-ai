package com.insureai.customer.adapter.inbound.web;

import java.util.List;

import com.insureai.customer.application.port.inbound.CustomerPage;

public record CustomerPageResponseDto(
    List<CustomerResponseDto> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {
    public static CustomerPageResponseDto fromDomain(CustomerPage customerPage) {
        return new CustomerPageResponseDto(
            customerPage.content().stream().map(CustomerResponseDto::fromDomain).toList(),
            customerPage.page(),
            customerPage.size(),
            customerPage.totalElements(),
            customerPage.totalPages()
        );
    }
}
