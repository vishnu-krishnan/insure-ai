package com.insureai.insurance.domain.model;

import java.util.Arrays;

import com.insureai.insurance.domain.exception.DomainValidationException;

public enum PaymentFrequency {
    MONTHLY(12),
    QUARTERLY(4),
    YEARLY(1);

    private final int periodsPerYear;

    PaymentFrequency(int periodsPerYear) {
        this.periodsPerYear = periodsPerYear;
    }

    public int periodsPerYear() {
        return periodsPerYear;
    }

    public static PaymentFrequency fromCode(String rawValue) {
        return Arrays.stream(values())
            .filter(frequency -> rawValue != null && frequency.name().equalsIgnoreCase(rawValue.trim()))
            .findFirst()
            .orElseThrow(() -> new DomainValidationException(
                "paymentFrequency must be one of MONTHLY, QUARTERLY, YEARLY", "paymentFrequency"));
    }
}
