package com.insureai.customer.domain.exception;

public class DuplicateCustomerException extends RuntimeException {
    private final String field;

    public DuplicateCustomerException(String field) {
        super("A customer with this " + ("phone".equals(field) ? "phone number" : field) + " already exists");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
