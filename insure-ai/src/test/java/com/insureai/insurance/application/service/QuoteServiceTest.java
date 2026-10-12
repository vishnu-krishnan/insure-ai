package com.insureai.insurance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.insureai.insurance.application.port.inbound.CreateQuoteCommand;
import com.insureai.insurance.application.port.outbound.QuoteRepositoryPort;
import com.insureai.insurance.domain.exception.DomainValidationException;
import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PolicyQuote;

class QuoteServiceTest {
    private final QuoteRepositoryPort repository = new InMemoryQuoteRepositoryForTesting();
    private final QuoteService quoteService = new QuoteService(repository);

    @Test
    void createQuoteUsesDomainValidationRules() {
        PolicyQuote quote = quoteService.createQuote(new CreateQuoteCommand("Alice Smith", "AUTO", 35, new BigDecimal("250000")));

        assertEquals("Alice Smith", quote.customerName());
        assertEquals(CoverageType.AUTO, quote.coverageType());
        assertEquals(new BigDecimal("17500.00"), quote.annualPremium());
        assertEquals(new BigDecimal("1458.33"), quote.monthlyPremium());
    }

    @Test
    void rejectsAgeOutsideAllowedRange() {
        DomainValidationException ex = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(new CreateQuoteCommand("Bob", "HOME", 17, new BigDecimal("100000"))));

        assertEquals("age must be between 18 and 75", ex.getMessage());
    }

    @Test
    void rejectsUnsupportedCoverageType() {
        DomainValidationException ex = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(new CreateQuoteCommand("Carol", "TRAVEL", 30, new BigDecimal("50000"))));

        assertEquals("coverageType must be one of AUTO, HOME, LIFE, HEALTH", ex.getMessage());
    }

    @Test
    void rejectsNonPositiveCoverageAmount() {
        DomainValidationException ex = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(new CreateQuoteCommand("Dana", "LIFE", 42, BigDecimal.ZERO)));

        assertEquals("coverageAmount must be greater than zero", ex.getMessage());
    }

    private static final class InMemoryQuoteRepositoryForTesting implements QuoteRepositoryPort {
        @Override
        public void save(PolicyQuote quote) {
        }

        @Override
        public java.util.List<PolicyQuote> findAll() {
            return java.util.List.of();
        }
    }
}
