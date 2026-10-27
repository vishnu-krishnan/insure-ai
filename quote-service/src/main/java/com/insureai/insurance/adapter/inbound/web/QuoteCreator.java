package com.insureai.insurance.adapter.inbound.web;

import org.springframework.security.oauth2.jwt.Jwt;

final class QuoteCreator {
    private static final int MAX_LENGTH = 100;

    private QuoteCreator() {
    }

    static String from(Jwt jwt) {
        if (jwt == null) {
            return null;
        }
        String creator = nonBlank(jwt.getClaimAsString("preferred_username"));
        if (creator == null) {
            creator = nonBlank(jwt.getSubject());
        }
        return creator == null || creator.length() <= MAX_LENGTH ? creator : creator.substring(0, MAX_LENGTH);
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
