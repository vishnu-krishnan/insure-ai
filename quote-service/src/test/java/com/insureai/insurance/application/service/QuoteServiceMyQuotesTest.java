package com.insureai.insurance.application.service;

import static com.insureai.insurance.QuoteFixtures.quote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

import com.insureai.insurance.application.port.inbound.CreateCustomerQuoteCommand;
import com.insureai.insurance.application.port.inbound.QuotePage;
import com.insureai.insurance.application.port.outbound.CustomerProfileLookupPort;
import com.insureai.insurance.application.port.outbound.CustomerProfileSnapshot;
import com.insureai.insurance.application.port.outbound.QuoteRepositoryPort;
import com.insureai.insurance.domain.exception.CustomerProfileMissingException;
import com.insureai.insurance.domain.exception.QuoteNotFoundException;
import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PolicyQuote;

@ExtendWith(MockitoExtension.class)
class QuoteServiceMyQuotesTest {
    private static final CustomerProfileSnapshot ALICE =
        new CustomerProfileSnapshot("10000001", "Alice Smith", LocalDate.of(1990, 1, 1), "CUS-000001");

    @Mock
    private QuoteRepositoryPort repository;

    @Mock
    private CustomerProfileLookupPort customerProfileLookup;

    private QuoteService quoteService;

    @BeforeEach
    void createService() {
        quoteService = new QuoteService(
            repository, customerProfileLookup,
            Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), ZoneOffset.UTC), new BigDecimal("0.18"));
    }

    @Test
    void listsOnlyTheQuotesOfTheCustomerLinkedToTheSubject() {
        PolicyQuote mine = quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00");
        when(customerProfileLookup.findCustomerBySubject("sub-1")).thenReturn(Optional.of(ALICE));
        when(repository.findPageForCustomer("10000001", 0L, 10)).thenReturn(List.of(mine));
        when(repository.countForCustomer("10000001")).thenReturn(1L);

        QuotePage page = quoteService.listMine("sub-1", 0, 10);

        assertEquals(List.of(mine), page.content());
        assertEquals(1L, page.totalElements());
    }

    @Test
    void aCustomerWithoutAProfileHasNoQuotes() {
        when(customerProfileLookup.findCustomerBySubject("sub-1")).thenReturn(Optional.empty());

        assertEquals(0L, quoteService.listMine("sub-1", 0, 10).totalElements());
        verify(repository, never()).findPageForCustomer(any(), org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void aQuoteOfAnotherCustomerIsNotFound() {
        when(customerProfileLookup.findCustomerBySubject("sub-1")).thenReturn(Optional.of(ALICE));
        when(repository.findByIdForCustomer("other-quote", "10000001")).thenReturn(Optional.empty());

        assertThrows(QuoteNotFoundException.class, () -> quoteService.getMine("sub-1", "other-quote"));
    }

    @Test
    void creatingAQuoteNeedsAProfile() {
        when(customerProfileLookup.findCustomerBySubject("sub-1")).thenReturn(Optional.empty());

        assertThrows(CustomerProfileMissingException.class, () -> quoteService.createMine("sub-1",
            new CreateCustomerQuoteCommand(null, "AUTO", new BigDecimal("250000"), null, null, null, null, "alice")));
        verify(repository, never()).save(any());
    }

    @Test
    void creatingAQuoteNeedsACompleteProfile() {
        when(customerProfileLookup.findCustomerBySubject("sub-1")).thenReturn(Optional.of(
            new CustomerProfileSnapshot("10000001", "Alice Smith", LocalDate.of(1990, 1, 1), "CUS-000001", false)));

        assertThrows(CustomerProfileMissingException.class, () -> quoteService.createMine("sub-1",
            new CreateCustomerQuoteCommand(null, "AUTO", new BigDecimal("250000"), null, null, null, null, "alice")));
        verify(repository, never()).save(any());
    }
}
