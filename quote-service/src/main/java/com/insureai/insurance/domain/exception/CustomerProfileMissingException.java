package com.insureai.insurance.domain.exception;

/** The signed-in customer has not completed their profile yet, so no quote can be tied to them. */
public class CustomerProfileMissingException extends RuntimeException {
    public CustomerProfileMissingException() {
        super("Complete your profile before requesting a quote");
    }
}
