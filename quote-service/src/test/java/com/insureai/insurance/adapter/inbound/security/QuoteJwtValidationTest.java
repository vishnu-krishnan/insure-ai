package com.insureai.insurance.adapter.inbound.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;

class QuoteJwtValidationTest {
    private static final Instant NOW = Instant.parse("2026-10-26T00:00:00Z");
    private static final String ISSUER = "https://issuer.example/realms/test";

    @Test
    void audienceValidatorRequiresTheQuoteServiceAudience() {
        assertFalse(QuoteSecurityConfiguration.audienceValidator("quote-service")
            .validate(jwt(List.of("quote-service"), NOW.minusSeconds(60), NOW.plusSeconds(60), ISSUER))
            .hasErrors());
        assertTrue(QuoteSecurityConfiguration.audienceValidator("quote-service")
            .validate(jwt(List.of("customer-service"), NOW.minusSeconds(60), NOW.plusSeconds(60), ISSUER))
            .hasErrors());
    }

    @Test
    void standardIssuerValidatorRejectsExpiredAndNotYetValidTokens() {
        var validator = JwtValidators.createDefaultWithIssuer(ISSUER);

        assertTrue(validator.validate(jwt(List.of("quote-service"), NOW.minusSeconds(120), NOW.minusSeconds(60), ISSUER))
            .hasErrors());
        assertTrue(validator.validate(jwt(List.of("quote-service"), NOW.plusSeconds(60), NOW.plusSeconds(120), ISSUER))
            .hasErrors());
        assertTrue(validator.validate(jwt(
            List.of("quote-service"),
            NOW.minusSeconds(60),
            NOW.plusSeconds(60),
            "https://issuer.example/realms/other"
        )).hasErrors());
    }

    private static Jwt jwt(List<String> audience, Instant notBefore, Instant expiresAt, String issuer) {
        return Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .issuer(issuer)
            .audience(audience)
            .notBefore(notBefore)
            .expiresAt(expiresAt)
            .issuedAt(notBefore.minusSeconds(30))
            .build();
    }
}
