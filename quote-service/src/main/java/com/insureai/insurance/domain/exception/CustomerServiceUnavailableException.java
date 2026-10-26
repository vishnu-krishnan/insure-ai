package com.insureai.insurance.domain.exception;

public class CustomerServiceUnavailableException extends RuntimeException {
    private final boolean retryable;

    public CustomerServiceUnavailableException(String message, Throwable cause, boolean retryable) {
        super(message, cause);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}
