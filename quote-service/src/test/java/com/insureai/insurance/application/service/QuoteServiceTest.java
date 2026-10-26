package com.insureai.insurance.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import com.insureai.insurance.application.port.inbound.CreateCustomerQuoteCommand;
import com.insureai.insurance.application.port.inbound.CreateQuoteCommand;
import com.insureai.insurance.application.port.inbound.QuotePage;
import com.insureai.insurance.application.port.outbound.CustomerProfileLookupPort;
import com.insureai.insurance.application.port.outbound.CustomerProfileSnapshot;
import com.insureai.insurance.application.port.outbound.QuoteRepositoryPort;
import com.insureai.insurance.domain.exception.CustomerReferenceNotFoundException;
import com.insureai.insurance.domain.exception.DomainValidationException;
import com.insureai.insurance.domain.exception.QuoteNotFoundException;
import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PolicyQuote;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {
    @Mock
    private QuoteRepositoryPort repository;

    @Mock
    private CustomerProfileLookupPort customerProfileLookup;

    private QuoteService quoteService;

    @BeforeEach
    void createService() {
        quoteService = new QuoteService(
            repository,
            customerProfileLookup,
            Clock.fixed(Instant.parse("2026-10-26T00:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void createQuoteUsesDomainValidationRules() {
        PolicyQuote quote = quoteService.createQuote(new CreateQuoteCommand("Alice Smith", "AUTO", 35, new BigDecimal("250000")));

        verify(repository).save(quote);
        assertEquals("Alice Smith", quote.customerName());
        assertEquals(CoverageType.AUTO, quote.coverageType());
        assertEquals(new BigDecimal("17500.00"), quote.annualPremium());
        assertEquals(new BigDecimal("1458.33"), quote.monthlyPremium());
    }

    @Test
    void listQuotesReturnsRequestedPageWithTotals() {
        PolicyQuote quote = new PolicyQuote(
            "quote-1",
            "Alice Smith",
            CoverageType.AUTO,
            35,
            new BigDecimal("250000"),
            new BigDecimal("17500.00")
        );
        when(repository.findPage(40L, 20)).thenReturn(List.of(quote));
        when(repository.count()).thenReturn(41L);

        QuotePage page = quoteService.listQuotes(2, 20);

        assertEquals(List.of(quote), page.content());
        assertEquals(2, page.page());
        assertEquals(20, page.size());
        assertEquals(41L, page.totalElements());
        assertEquals(3, page.totalPages());
    }

    @Test
    void listQuotesRejectsInvalidPagingParameters() {
        assertThrows(DomainValidationException.class, () -> quoteService.listQuotes(-1, 20));
        assertThrows(DomainValidationException.class, () -> quoteService.listQuotes(0, 0));
        assertThrows(DomainValidationException.class,
            () -> quoteService.listQuotes(0, QuoteService.MAX_PAGE_SIZE + 1));
        verifyNoInteractions(repository);
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
        verifyNoInteractions(repository);
    }

    @Test
    void acceptsAgeBoundaries() {
        quoteService.createQuote(new CreateQuoteCommand("Age Eighteen", "AUTO", 18, BigDecimal.ONE));
        quoteService.createQuote(new CreateQuoteCommand("Age Seventy Five", "AUTO", 75, BigDecimal.ONE));

        verify(repository, times(2)).save(any(PolicyQuote.class));
    }

    @Test
    void propagatesRepositoryFailures() {
        DataAccessResourceFailureException failure =
            new DataAccessResourceFailureException("database unavailable");
        doThrow(failure).when(repository).save(any(PolicyQuote.class));

        DataAccessResourceFailureException thrown = assertThrows(DataAccessResourceFailureException.class,
            () -> quoteService.createQuote(new CreateQuoteCommand(
                "Alice Smith", "AUTO", 35, new BigDecimal("250000"))));

        assertEquals(failure, thrown);
    }

    @Test
    void retrievesQuoteById() {
        PolicyQuote expected = new PolicyQuote(
            "quote-1",
            "Alice Smith",
            CoverageType.AUTO,
            35,
            new BigDecimal("250000"),
            new BigDecimal("17500.00")
        );
        when(repository.findById("quote-1")).thenReturn(Optional.of(expected));

        assertEquals(expected, quoteService.getQuoteById("quote-1"));
        verify(repository).findById("quote-1");
    }

    @Test
    void reportsMissingQuoteById() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        QuoteNotFoundException thrown = assertThrows(
            QuoteNotFoundException.class,
            () -> quoteService.getQuoteById("missing")
        );

        assertEquals("Quote missing was not found", thrown.getMessage());
    }

    @Test
    void createsQuoteUsingCustomerProfileAndPersistsSnapshot() {
        String customerId = "3b241101-e2bb-4255-8caf-4136c566a962";
        when(customerProfileLookup.findCustomer(customerId)).thenReturn(
            new CustomerProfileSnapshot(customerId, "Alice Smith", LocalDate.of(1990, 10, 27))
        );

        PolicyQuote quote = quoteService.createQuoteForCustomer(
            new CreateCustomerQuoteCommand(customerId, "AUTO", new BigDecimal("250000"))
        );

        assertEquals(35, quote.age());
        assertEquals("Alice Smith", quote.customerName());
        assertEquals(customerId, quote.customerId());
        verify(repository).save(quote);
    }

    @Test
    void rejectsUnknownCustomerAndDoesNotPersistQuote() {
        String customerId = "3b241101-e2bb-4255-8caf-4136c566a962";
        when(customerProfileLookup.findCustomer(customerId))
            .thenThrow(new CustomerReferenceNotFoundException(customerId));

        assertThrows(
            CustomerReferenceNotFoundException.class,
            () -> quoteService.createQuoteForCustomer(
                new CreateCustomerQuoteCommand(customerId, "AUTO", BigDecimal.TEN)
            )
        );
        verifyNoInteractions(repository);
    }
}
