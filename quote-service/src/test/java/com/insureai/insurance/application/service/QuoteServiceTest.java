package com.insureai.insurance.application.service;

import static com.insureai.insurance.QuoteFixtures.quote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import com.insureai.insurance.domain.model.PaymentFrequency;
import com.insureai.insurance.domain.model.PolicyQuote;

@ExtendWith(MockitoExtension.class)
class QuoteServiceTest {
    @Mock
    private QuoteRepositoryPort repository;

    @Mock
    private CustomerProfileLookupPort customerProfileLookup;

    private static final Instant NOW = Instant.parse("2026-10-26T00:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);

    private QuoteService quoteService;

    @BeforeEach
    void createService() {
        lenient().when(repository.save(any(PolicyQuote.class))).thenAnswer(invocation -> invocation.getArgument(0));
        quoteService = new QuoteService(
            repository,
            customerProfileLookup,
            Clock.fixed(NOW, ZoneOffset.UTC),
            new BigDecimal("0.18")
        );
    }

    @Test
    void createQuoteUsesDomainValidationRules() {
        PolicyQuote quote = quoteService.createQuote(command("Alice Smith", "AUTO", 35, new BigDecimal("250000")));

        verify(repository).save(any(PolicyQuote.class));
        assertEquals("Alice Smith", quote.customerName());
        assertEquals(CoverageType.AUTO, quote.coverageType());
        assertEquals(new BigDecimal("17500.00"), quote.annualPremium());
        assertEquals(new BigDecimal("1458.33"), quote.monthlyPremium());
    }

    @Test
    void listQuotesReturnsRequestedPageWithTotals() {
        PolicyQuote quote = quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00");
        when(repository.findPage("", null, 40L, 20)).thenReturn(List.of(quote));
        when(repository.count("", null)).thenReturn(41L);

        QuotePage page = quoteService.listQuotes(2, 20, null, null);

        assertEquals(List.of(quote), page.content());
        assertEquals(2, page.page());
        assertEquals(20, page.size());
        assertEquals(41L, page.totalElements());
        assertEquals(3, page.totalPages());
    }

    @Test
    void listQuotesRejectsInvalidPagingParameters() {
        assertThrows(DomainValidationException.class, () -> quoteService.listQuotes(-1, 20, null, null));
        assertThrows(DomainValidationException.class, () -> quoteService.listQuotes(0, 0, null, null));
        assertThrows(DomainValidationException.class,
            () -> quoteService.listQuotes(0, QuoteService.MAX_PAGE_SIZE + 1, null, null));
        verifyNoInteractions(repository);
    }

    @Test
    void listQuotesPassesTrimmedSearchTermToRepository() {
        when(repository.findPage("alice", null, 0L, 20)).thenReturn(List.of());
        when(repository.count("alice", null)).thenReturn(0L);

        QuotePage page = quoteService.listQuotes(0, 20, "  alice ", null);

        assertEquals(0, page.totalElements());
        verify(repository).findPage("alice", null, 0L, 20);
    }

    @Test
    void listQuotesTreatsBlankSearchTermAsNoFilter() {
        when(repository.findPage("", null, 0L, 20)).thenReturn(List.of());
        when(repository.count("", null)).thenReturn(0L);

        quoteService.listQuotes(0, 20, "   ", null);

        verify(repository).findPage("", null, 0L, 20);
    }

    @Test
    void listQuotesRejectsSearchTermLongerThanMaximum() {
        DomainValidationException ex = assertThrows(DomainValidationException.class,
            () -> quoteService.listQuotes(0, 20, "x".repeat(QuoteService.MAX_SEARCH_LENGTH + 1), null));

        assertEquals("q must be at most 80 characters", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsAgeOutsideAllowedRange() {
        DomainValidationException ex = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(command("Bob", "HOME", 17, new BigDecimal("100000"))));

        assertEquals("age must be between 18 and 75", ex.getMessage());
    }

    @Test
    void rejectsUnsupportedCoverageType() {
        DomainValidationException ex = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(command("Carol", "TRAVEL", 30, new BigDecimal("50000"))));

        assertEquals("coverageType must be one of AUTO, HOME, LIFE, HEALTH", ex.getMessage());
    }

    @Test
    void rejectsNonPositiveCoverageAmount() {
        DomainValidationException ex = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(command("Dana", "LIFE", 42, BigDecimal.ZERO)));

        assertEquals("coverageAmount must be greater than zero", ex.getMessage());
        verifyNoInteractions(repository);
    }

    @Test
    void acceptsAgeBoundaries() {
        quoteService.createQuote(command("Age Eighteen", "AUTO", 18, BigDecimal.ONE));
        quoteService.createQuote(command("Age Seventy Five", "AUTO", 75, BigDecimal.ONE));

        verify(repository, times(2)).save(any(PolicyQuote.class));
    }

    @Test
    void propagatesRepositoryFailures() {
        DataAccessResourceFailureException failure =
            new DataAccessResourceFailureException("database unavailable");
        doThrow(failure).when(repository).save(any(PolicyQuote.class));

        DataAccessResourceFailureException thrown = assertThrows(DataAccessResourceFailureException.class,
            () -> quoteService.createQuote(command("Alice Smith", "AUTO", 35, new BigDecimal("250000"))));

        assertEquals(failure, thrown);
    }

    @Test
    void retrievesQuoteById() {
        PolicyQuote expected = quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00");
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
            new CustomerProfileSnapshot(customerId, "Alice Smith", LocalDate.of(1990, 10, 27), null)
        );

        PolicyQuote quote = quoteService.createQuoteForCustomer(
            customerCommand(customerId, "AUTO", new BigDecimal("250000"))
        );

        assertEquals(35, quote.age());
        assertEquals("Alice Smith", quote.customerName());
        assertEquals(customerId, quote.customerId());
        verify(repository).save(quote);
    }

    @Test
    void copiesCustomerNumberFromProfileSnapshot() {
        String customerId = "3b241101-e2bb-4255-8caf-4136c566a962";
        when(customerProfileLookup.findCustomer(customerId)).thenReturn(
            new CustomerProfileSnapshot(customerId, "Alice Smith", LocalDate.of(1990, 10, 27), "CUS-000001")
        );

        PolicyQuote quote = quoteService.createQuoteForCustomer(
            customerCommand(customerId, "AUTO", new BigDecimal("250000"))
        );

        assertEquals("CUS-000001", quote.customerNumber());
    }

    @Test
    void returnsQuoteNumberAssignedByRepository() {
        when(repository.save(any(PolicyQuote.class))).thenAnswer(invocation -> {
            PolicyQuote unsaved = invocation.getArgument(0);
            return unsaved.withQuoteNumber("QT-000009");
        });

        PolicyQuote quote = quoteService.createQuote(
            command("Alice Smith", "AUTO", 35, new BigDecimal("250000")));

        assertEquals("QT-000009", quote.quoteNumber());
        assertNull(quote.customerNumber());
    }

    @Test
    void rejectsUnknownCustomerAndDoesNotPersistQuote() {
        String customerId = "3b241101-e2bb-4255-8caf-4136c566a962";
        when(customerProfileLookup.findCustomer(customerId))
            .thenThrow(new CustomerReferenceNotFoundException(customerId));

        assertThrows(
            CustomerReferenceNotFoundException.class,
            () -> quoteService.createQuoteForCustomer(
                customerCommand(customerId, "AUTO", BigDecimal.TEN)
            )
        );
        verifyNoInteractions(repository);
    }

    @Test
    void appliesDefaultsWhenOptionalDetailsAreOmitted() {
        PolicyQuote quote = quoteService.createQuote(command("Alice Smith", "AUTO", 35, new BigDecimal("250000")));

        assertEquals(1, quote.policyTermYears());
        assertEquals(PaymentFrequency.YEARLY, quote.paymentFrequency());
        assertEquals(new BigDecimal("0.00"), quote.deductible());
        assertEquals(TODAY, quote.startDate());
        assertEquals(new BigDecimal("0.1800"), quote.taxRate());
        assertEquals(NOW, quote.createdAt());
        assertNull(quote.createdBy());
    }

    @Test
    void keepsSuppliedDetailsAndCreator() {
        PolicyQuote quote = quoteService.createQuote(new CreateQuoteCommand(
            "Alice Smith", "AUTO", 35, new BigDecimal("250000"),
            10, "monthly", new BigDecimal("500.5"), TODAY.plusDays(30), "alice"));

        assertEquals(10, quote.policyTermYears());
        assertEquals(PaymentFrequency.MONTHLY, quote.paymentFrequency());
        assertEquals(new BigDecimal("500.50"), quote.deductible());
        assertEquals(TODAY.plusDays(30), quote.startDate());
        assertEquals("alice", quote.createdBy());
    }

    @Test
    void customerQuoteAppliesDefaultsAndCarriesDetails() {
        String customerId = "3b241101-e2bb-4255-8caf-4136c566a962";
        when(customerProfileLookup.findCustomer(customerId)).thenReturn(
            new CustomerProfileSnapshot(customerId, "Alice Smith", LocalDate.of(1990, 10, 27), null)
        );

        PolicyQuote defaults = quoteService.createQuoteForCustomer(
            customerCommand(customerId, "AUTO", new BigDecimal("250000")));
        PolicyQuote detailed = quoteService.createQuoteForCustomer(new CreateCustomerQuoteCommand(
            customerId, "AUTO", new BigDecimal("250000"), 5, "QUARTERLY", new BigDecimal("1000"), TODAY, "sub-1"));

        assertEquals(1, defaults.policyTermYears());
        assertEquals(PaymentFrequency.YEARLY, defaults.paymentFrequency());
        assertEquals(5, detailed.policyTermYears());
        assertEquals(PaymentFrequency.QUARTERLY, detailed.paymentFrequency());
        assertEquals("sub-1", detailed.createdBy());
    }

    @Test
    void rejectsPolicyTermOutsideRange() {
        for (int term : new int[] {0, 41, -1}) {
            DomainValidationException ex = assertThrows(DomainValidationException.class,
                () -> quoteService.createQuote(withDetails(term, null, null, null)));
            assertEquals("policyTermYears must be between 1 and 40", ex.getMessage());
            assertEquals("policyTermYears", ex.field());
        }
        quoteService.createQuote(withDetails(1, null, null, null));
        quoteService.createQuote(withDetails(40, null, null, null));
    }

    @Test
    void rejectsUnknownPaymentFrequency() {
        DomainValidationException ex = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(withDetails(null, "WEEKLY", null, null)));

        assertEquals("paymentFrequency must be one of MONTHLY, QUARTERLY, YEARLY", ex.getMessage());
        assertEquals("paymentFrequency", ex.field());
    }

    @Test
    void rejectsNegativeDeductible() {
        DomainValidationException ex = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(withDetails(null, null, new BigDecimal("-0.01"), null)));

        assertEquals("deductible must be zero or greater", ex.getMessage());
        assertEquals("deductible", ex.field());
    }

    @Test
    void rejectsDeductibleNotBelowCoverageAmount() {
        DomainValidationException equal = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(withDetails(null, null, new BigDecimal("250000"), null)));
        assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(withDetails(null, null, new BigDecimal("250000.01"), null)));

        assertEquals("deductible must be less than coverageAmount", equal.getMessage());
        assertEquals("deductible", equal.field());
        quoteService.createQuote(withDetails(null, null, new BigDecimal("249999.99"), null));
    }

    @Test
    void rejectsStartDateOutsideWindow() {
        DomainValidationException past = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(withDetails(null, null, null, TODAY.minusDays(1))));
        DomainValidationException future = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(withDetails(null, null, null, TODAY.plusDays(366))));

        assertEquals("startDate must be between today and 365 days from today", past.getMessage());
        assertEquals("startDate", past.field());
        assertEquals("startDate", future.field());
        quoteService.createQuote(withDetails(null, null, null, TODAY));
        quoteService.createQuote(withDetails(null, null, null, TODAY.plusDays(365)));
    }

    @Test
    void startDateWindowFollowsUtcDateOfClock() {
        QuoteService lateEvening = new QuoteService(
            repository, customerProfileLookup,
            Clock.fixed(Instant.parse("2026-10-26T23:59:59Z"), ZoneOffset.UTC), new BigDecimal("0.18"));

        assertThrows(DomainValidationException.class,
            () -> lateEvening.createQuote(withDetails(null, null, null, TODAY.minusDays(1))));
        assertEquals(TODAY, lateEvening.createQuote(withDetails(null, null, null, null)).startDate());
    }

    @Test
    void computesTaxTotalAndInstallmentForEachFrequency() {
        PolicyQuote yearly = quoteService.createQuote(premiumOf12500("YEARLY"));
        PolicyQuote quarterly = quoteService.createQuote(premiumOf12500("QUARTERLY"));
        PolicyQuote monthly = quoteService.createQuote(premiumOf12500("MONTHLY"));

        assertEquals(new BigDecimal("12500.00"), yearly.annualPremium());
        assertEquals(new BigDecimal("2250.00"), yearly.taxAmount());
        assertEquals(new BigDecimal("14750.00"), yearly.totalAnnualPayable());
        assertEquals(new BigDecimal("14750.00"), yearly.installmentAmount());
        assertEquals(new BigDecimal("3687.50"), quarterly.installmentAmount());
        assertEquals(new BigDecimal("1229.17"), monthly.installmentAmount());
        assertEquals(new BigDecimal("1041.67"), monthly.monthlyPremium());
    }

    @Test
    void roundsTaxHalfUp() {
        QuoteService service = new QuoteService(
            repository, customerProfileLookup, Clock.fixed(NOW, ZoneOffset.UTC), new BigDecimal("0.0725"));

        PolicyQuote quote = service.createQuote(command("Alice Smith", "AUTO", 35, new BigDecimal("1000.50")));

        assertEquals(new BigDecimal("70.04"), quote.annualPremium());
        assertEquals(new BigDecimal("0.0725"), quote.taxRate());
        assertEquals(new BigDecimal("5.08"), quote.taxAmount());
        assertEquals(new BigDecimal("75.12"), quote.totalAnnualPayable());
    }

    @Test
    void storesZeroTaxWhenRateIsZero() {
        QuoteService service = new QuoteService(
            repository, customerProfileLookup, Clock.fixed(NOW, ZoneOffset.UTC), BigDecimal.ZERO);

        PolicyQuote quote = service.createQuote(premiumOf12500("YEARLY"));

        assertEquals(new BigDecimal("0.00"), quote.taxAmount());
        assertEquals(quote.annualPremium(), quote.totalAnnualPayable());
    }

    @Test
    void rejectsTaxRateOutsideZeroToOne() {
        assertThrows(IllegalArgumentException.class, () -> new QuoteService(
            repository, customerProfileLookup, Clock.fixed(NOW, ZoneOffset.UTC), new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> new QuoteService(
            repository, customerProfileLookup, Clock.fixed(NOW, ZoneOffset.UTC), new BigDecimal("1.01")));
        assertThrows(IllegalArgumentException.class, () -> new QuoteService(
            repository, customerProfileLookup, Clock.fixed(NOW, ZoneOffset.UTC), new BigDecimal("0.12345")));
        assertThrows(NullPointerException.class, () -> new QuoteService(
            repository, customerProfileLookup, Clock.fixed(NOW, ZoneOffset.UTC), null));
        new QuoteService(repository, customerProfileLookup, Clock.fixed(NOW, ZoneOffset.UTC), BigDecimal.ONE);
    }

    @Test
    void validUntilIsThirtyDaysAfterCreationDateInUtc() {
        QuoteService service = new QuoteService(
            repository, customerProfileLookup,
            Clock.fixed(Instant.parse("2026-12-15T23:30:00Z"), ZoneOffset.UTC), new BigDecimal("0.18"));

        PolicyQuote quote = service.createQuote(command("Alice Smith", "AUTO", 35, new BigDecimal("250000")));

        assertEquals(LocalDate.of(2027, 1, 14), quote.validUntil());
    }

    @Test
    void truncatesCreationInstantToMicroseconds() {
        QuoteService service = new QuoteService(
            repository, customerProfileLookup,
            Clock.fixed(Instant.parse("2026-10-26T00:00:00.123456789Z"), ZoneOffset.UTC), new BigDecimal("0.18"));

        PolicyQuote quote = service.createQuote(command("Alice Smith", "AUTO", 35, new BigDecimal("250000")));

        assertEquals(Instant.parse("2026-10-26T00:00:00.123456Z"), quote.createdAt());
    }

    @Test
    void createQuoteWithoutTypeDetailsKeepsThemNull() {
        PolicyQuote quote = quoteService.createQuote(command("Alice Smith", "AUTO", 35, new BigDecimal("250000")));

        assertNull(quote.typeDetails());
    }

    @Test
    void createQuoteStoresValidatedTypeDetailsWithoutChangingPremium() {
        PolicyQuote plain = quoteService.createQuote(command("Alice Smith", "AUTO", 35, new BigDecimal("250000")));
        PolicyQuote detailed = quoteService.createQuote(commandWithTypeDetails("AUTO", autoDetails()));

        assertEquals(autoDetails(), detailed.typeDetails());
        assertEquals(plain.annualPremium(), detailed.annualPremium());
        assertEquals(plain.taxAmount(), detailed.taxAmount());
    }

    @Test
    void createQuoteRejectsInvalidTypeDetailsNamingTheField() {
        Map<String, Object> invalid = autoDetails();
        invalid.put("vehicleYear", 1900);

        DomainValidationException exception = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(commandWithTypeDetails("AUTO", invalid)));

        assertEquals("details.vehicleYear", exception.field());
        verifyNoInteractions(repository);
    }

    @Test
    void createQuoteRejectsDetailsForTheWrongCoverageType() {
        DomainValidationException exception = assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(commandWithTypeDetails("HOME", autoDetails())));

        assertEquals("details.vehicleMake", exception.field());
    }

    @Test
    void vehicleYearUpperBoundFollowsTheClockYear() {
        Map<String, Object> nextYear = autoDetails();
        nextYear.put("vehicleYear", 2027);
        Map<String, Object> tooFar = autoDetails();
        tooFar.put("vehicleYear", 2028);

        assertEquals(2027, quoteService.createQuote(commandWithTypeDetails("AUTO", nextYear))
            .typeDetails().get("vehicleYear"));
        assertThrows(DomainValidationException.class,
            () -> quoteService.createQuote(commandWithTypeDetails("AUTO", tooFar)));
    }

    @Test
    void customerQuoteValidatesAndStoresTypeDetails() {
        String customerId = "3b241101-e2bb-4255-8caf-4136c566a962";
        when(customerProfileLookup.findCustomer(customerId)).thenReturn(
            new CustomerProfileSnapshot(customerId, "Alice Smith", LocalDate.of(1990, 10, 27), null));
        Map<String, Object> health = Map.of(
            "members", 3, "preExistingConditions", false, "smoker", false, "planTier", "PREMIUM");

        PolicyQuote quote = quoteService.createQuoteForCustomer(new CreateCustomerQuoteCommand(
            customerId, "HEALTH", new BigDecimal("250000"), null, null, null, null, null, health));

        assertEquals(health, quote.typeDetails());
        assertThrows(DomainValidationException.class, () -> quoteService.createQuoteForCustomer(
            new CreateCustomerQuoteCommand(customerId, "AUTO", new BigDecimal("250000"),
                null, null, null, null, null, health)));
    }

    @Test
    void listQuotesFiltersByCoverageTypeCaseInsensitively() {
        PolicyQuote home = quote("quote-1", "Alice Smith", CoverageType.HOME, 35, "250000", "12500.00");
        when(repository.findPage("alice", CoverageType.HOME, 0L, 20)).thenReturn(List.of(home));
        when(repository.count("alice", CoverageType.HOME)).thenReturn(1L);

        QuotePage page = quoteService.listQuotes(0, 20, "alice", " home ");

        assertEquals(List.of(home), page.content());
        assertEquals(1L, page.totalElements());
    }

    @Test
    void listQuotesRejectsUnknownCoverageType() {
        assertThrows(DomainValidationException.class, () -> quoteService.listQuotes(0, 20, null, "BOAT"));
        assertThrows(DomainValidationException.class, () -> quoteService.listQuotes(0, 20, null, ""));
        verifyNoInteractions(repository);
    }

    private static Map<String, Object> autoDetails() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("vehicleMake", "Toyota");
        map.put("vehicleModel", "Corolla");
        map.put("vehicleYear", 2020);
        map.put("registrationNumber", "KA01AB1234");
        map.put("usage", "PRIVATE");
        map.put("previousClaims", 1);
        return map;
    }

    private static CreateQuoteCommand commandWithTypeDetails(String coverageType, Map<String, Object> details) {
        return new CreateQuoteCommand(
            "Alice Smith", coverageType, 35, new BigDecimal("250000"), null, null, null, null, null, details);
    }

    private static CreateQuoteCommand command(String name, String coverageType, int age, BigDecimal amount) {
        return new CreateQuoteCommand(name, coverageType, age, amount, null, null, null, null, null);
    }

    private static CreateCustomerQuoteCommand customerCommand(String customerId, String coverageType, BigDecimal amount) {
        return new CreateCustomerQuoteCommand(customerId, coverageType, amount, null, null, null, null, null);
    }

    private static CreateQuoteCommand withDetails(
        Integer term, String frequency, BigDecimal deductible, LocalDate startDate
    ) {
        return new CreateQuoteCommand(
            "Alice Smith", "AUTO", 35, new BigDecimal("250000"), term, frequency, deductible, startDate, null);
    }

    private static CreateQuoteCommand premiumOf12500(String frequency) {
        return new CreateQuoteCommand(
            "Alice Smith", "HOME", 35, new BigDecimal("250000"), null, frequency, null, null, null);
    }
}
