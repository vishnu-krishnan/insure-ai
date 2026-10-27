package com.insureai.customer.application.port.inbound;

public interface SearchCustomersUseCase {
    CustomerPage searchCustomers(String text, int page, int size);
}
