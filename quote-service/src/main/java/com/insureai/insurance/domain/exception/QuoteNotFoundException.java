package com.insureai.insurance.domain.exception;

public class QuoteNotFoundException extends RuntimeException {
    public QuoteNotFoundException(String quoteId) {
        super("Quote " + quoteId + " was not found");
    }
}
