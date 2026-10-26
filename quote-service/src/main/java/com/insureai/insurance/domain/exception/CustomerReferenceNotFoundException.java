package com.insureai.insurance.domain.exception;

public class CustomerReferenceNotFoundException extends RuntimeException {
    public CustomerReferenceNotFoundException(String customerId) {
        super("Customer " + customerId + " was not found");
    }
}
