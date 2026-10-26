package com.insureai.insurance.domain.exception;

public class CustomerServiceTimeoutException extends RuntimeException {
    public CustomerServiceTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
