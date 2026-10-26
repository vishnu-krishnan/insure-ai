package com.insureai.insurance.application.port.outbound;

public interface CustomerProfileLookupPort {
    CustomerProfileSnapshot findCustomer(String customerId);
}
