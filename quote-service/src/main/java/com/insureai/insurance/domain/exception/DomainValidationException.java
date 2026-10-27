package com.insureai.insurance.domain.exception;

public class DomainValidationException extends RuntimeException {
    private final String field;

    public DomainValidationException(String message) {
        this(message, null);
    }

    public DomainValidationException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
