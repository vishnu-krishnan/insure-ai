package com.insureai.insurance.domain.model;

import static com.insureai.insurance.QuoteFixtures.quote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class PolicyQuoteTest {
    private final PolicyQuote quote = quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00");

    @Test
    void validUntilIsThirtyDaysAfterCreationDate() {
        assertEquals(LocalDate.of(2026, 11, 25), quote.validUntil());
    }

    @Test
    void statusIsIssuedUpToAndIncludingValidUntil() {
        assertEquals(QuoteStatus.ISSUED, quote.status(LocalDate.of(2026, 10, 26)));
        assertEquals(QuoteStatus.ISSUED, quote.status(LocalDate.of(2026, 11, 25)));
    }

    @Test
    void statusIsExpiredAfterValidUntil() {
        assertEquals(QuoteStatus.EXPIRED, quote.status(LocalDate.of(2026, 11, 26)));
    }

    @Test
    void periodsPerYearMatchFrequency() {
        assertEquals(12, PaymentFrequency.MONTHLY.periodsPerYear());
        assertEquals(4, PaymentFrequency.QUARTERLY.periodsPerYear());
        assertEquals(1, PaymentFrequency.YEARLY.periodsPerYear());
    }

    @Test
    void requiresPersistedDetails() {
        assertThrows(NullPointerException.class, () -> new PolicyQuote(
            "quote-1", "Alice Smith", CoverageType.AUTO, 35, quote.coverageAmount(), quote.annualPremium(),
            null, null, null, 1, null, quote.deductible(), quote.startDate(), quote.taxRate(),
            quote.taxAmount(), quote.createdAt(), null));
    }
}
