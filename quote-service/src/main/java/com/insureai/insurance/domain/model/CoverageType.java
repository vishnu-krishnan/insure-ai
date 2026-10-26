package com.insureai.insurance.domain.model;

import java.math.BigDecimal;
import java.util.Arrays;

import com.insureai.insurance.domain.exception.DomainValidationException;

public enum CoverageType {
    AUTO("AUTO", new BigDecimal("0.07")),
    HOME("HOME", new BigDecimal("0.05")),
    LIFE("LIFE", new BigDecimal("0.09")),
    HEALTH("HEALTH", new BigDecimal("0.08"));

    private final String code;
    private final BigDecimal baseRate;

    CoverageType(String code, BigDecimal baseRate) {
        this.code = code;
        this.baseRate = baseRate;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getBaseRate() {
        return baseRate;
    }

    public static CoverageType fromCode(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new DomainValidationException("coverageType is required");
        }

        return Arrays.stream(values())
            .filter(type -> type.code.equalsIgnoreCase(rawValue.trim()))
            .findFirst()
            .orElseThrow(() -> new DomainValidationException(
                "coverageType must be one of AUTO, HOME, LIFE, HEALTH"));
    }
}
