package com.insureai.insurance.adapter.outbound.persistence;

import static com.insureai.insurance.QuoteFixtures.quote;
import static com.insureai.insurance.QuoteFixtures.withCreatedBy;
import static com.insureai.insurance.QuoteFixtures.withTypeDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.insureai.insurance.application.port.outbound.QuoteRepositoryPort;
import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PaymentFrequency;
import com.insureai.insurance.domain.model.PolicyQuote;

@SpringBootTest(properties = {
    "security.jwt.issuer-uri=https://issuer.example/realms/test",
    "security.jwt.audience=quote-service",
    "spring.security.oauth2.client.registration.customer-service.client-id=test-quote-client",
    "spring.security.oauth2.client.registration.customer-service.client-secret=test-quote-secret",
    "spring.security.oauth2.client.provider.keycloak.token-uri=https://issuer.example/token"
})
@Testcontainers
class PostgresQuoteRepositoryIntegrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
        .withDatabaseName("insure_test_db")
        .withUsername("insure_test_admin")
        .withPassword("test-only-insure-admin-password")
        .withInitScript("db/test-container-init.sql");

    private static final String CUSTOMER_ID = "3b241101-e2bb-4255-8caf-4136c566a962";

    @Autowired
    private QuoteRepositoryPort quoteRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private Flyway flyway;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("INSURE_DB_URL", POSTGRES::getJdbcUrl);
        registry.add("INSURE_DB_USERNAME", () -> "insure_test_app");
        registry.add("INSURE_DB_PASSWORD", () -> "test-only-insure-app-password");
    }

    @BeforeEach
    void clearQuotes() {
        jdbcTemplate.update("DELETE FROM policy_quote");
    }

    @Test
    void flywayMigratesFreshPostgresDatabase() {
        assertEquals("5", flyway.info().current().getVersion().getVersion());
        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'policy_quote'",
            Integer.class
        ));
    }

    @Test
    void savesAndRetrievesQuoteWithDomainFieldsMapped() {
        PolicyQuote saved = quoteRepository.save(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        assertTrue(saved.quoteNumber().matches("QT-[0-9]{6,}"));
        assertEquals(List.of(saved), quoteRepository.findPage("", null, 0, 10));
    }

    @Test
    void savesCustomerNumberAndReturnsStoredQuoteNumber() {
        PolicyQuote saved = quoteRepository.save(quote(
            "quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00", CUSTOMER_ID, null, "CUS-000007"));

        assertEquals("CUS-000007", saved.customerNumber());
        assertEquals(Optional.of(saved), quoteRepository.findById("quote-1"));
    }

    @Test
    void assignsIncreasingQuoteNumbersOnInsert() {
        PolicyQuote first = quoteRepository.save(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        PolicyQuote second = quoteRepository.save(quote("quote-2", "Bob Jones", CoverageType.HOME, 65, "400000", "23000.00"));

        assertTrue(first.quoteNumber().compareTo(second.quoteNumber()) < 0);
    }

    @Test
    void quoteNumbersStayDistinctAndNewestFirstPastSixDigits() {
        jdbcTemplate.queryForObject("SELECT setval('quote_number_seq', 999998)", Long.class);
        try {
            PolicyQuote sixDigits = quoteRepository.save(
                quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
            PolicyQuote sevenDigits = quoteRepository.save(
                quote("quote-2", "Bob Jones", CoverageType.AUTO, 35, "250000", "17500.00"));
            PolicyQuote eightDigits = quoteRepository.save(
                quote("quote-3", "Carol White", CoverageType.AUTO, 35, "250000", "17500.00"));

            assertEquals("QT-999999", sixDigits.quoteNumber());
            assertEquals("QT-1000000", sevenDigits.quoteNumber());
            assertEquals("QT-1000001", eightDigits.quoteNumber());
            assertEquals(
                List.of(eightDigits, sevenDigits, sixDigits),
                quoteRepository.findPage("", null, 0, 10)
            );
        } finally {
            jdbcTemplate.queryForObject("SELECT setval('quote_number_seq', 1, false)", Long.class);
        }
    }

    @Test
    void findsQuoteByIdAndReturnsEmptyForUnknownId() {
        PolicyQuote expected = quoteRepository.save(
            quote("quote-by-id", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        assertEquals(Optional.of(expected), quoteRepository.findById("quote-by-id"));
        assertEquals(Optional.empty(), quoteRepository.findById("unknown"));
    }

    @Test
    void pagesQuotesNewestFirstAndCountsThem() {
        assertTrue(quoteRepository.findPage("", null, 0, 10).isEmpty());
        assertEquals(0, quoteRepository.count("", null));
        PolicyQuote first = quoteRepository.save(quote("quote-3", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        PolicyQuote second = quoteRepository.save(quote("quote-1", "Bob Jones", CoverageType.HOME, 65, "400000", "23000.00"));
        PolicyQuote third = quoteRepository.save(quote("quote-2", "Carol White", CoverageType.LIFE, 40, "300000", "27000.00"));

        assertEquals(3, quoteRepository.count("", null));
        assertEquals(List.of(third, second), quoteRepository.findPage("", null, 0, 2));
        assertEquals(List.of(first), quoteRepository.findPage("", null, 2, 2));
        assertTrue(quoteRepository.findPage("", null, 4, 2).isEmpty());
    }

    @Test
    void searchesByQuoteNumberCaseInsensitively() {
        quoteRepository.save(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        PolicyQuote target = quoteRepository.save(quote("quote-2", "Bob Jones", CoverageType.AUTO, 35, "250000", "17500.00"));

        assertEquals(List.of(target), quoteRepository.findPage(target.quoteNumber().toLowerCase(), null, 0, 10));
        assertEquals(1, quoteRepository.count(target.quoteNumber().toLowerCase(), null));
    }

    @Test
    void searchesByCustomerNameFragmentCaseInsensitively() {
        PolicyQuote alice = quoteRepository.save(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        quoteRepository.save(quote("quote-2", "Bob Jones", CoverageType.AUTO, 35, "250000", "17500.00"));

        assertEquals(List.of(alice), quoteRepository.findPage("ICE sm", null, 0, 10));
    }

    @Test
    void searchesByCustomerNumberAndCustomerId() {
        PolicyQuote linked = quoteRepository.save(quote(
            "quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00", CUSTOMER_ID, null, "CUS-000042"));
        quoteRepository.save(quote("quote-2", "Bob Jones", CoverageType.AUTO, 35, "250000", "17500.00"));

        assertEquals(List.of(linked), quoteRepository.findPage("cus-000042", null, 0, 10));
        assertEquals(List.of(linked), quoteRepository.findPage(CUSTOMER_ID.substring(0, 8), null, 0, 10));
    }

    @Test
    void searchesByQuoteIdPrefix() {
        String quoteId = "9f1c2d3e-0000-4000-8000-000000000001";
        PolicyQuote target = quoteRepository.save(quote(quoteId, "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        quoteRepository.save(quote("7a7a7a7a-0000-4000-8000-000000000002", "Bob Jones", CoverageType.AUTO, 35, "250000", "17500.00"));

        assertEquals(List.of(target), quoteRepository.findPage("9f1c2d3e", null, 0, 10));
    }

    @Test
    void searchesByCoverageType() {
        quoteRepository.save(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        PolicyQuote home = quoteRepository.save(quote("quote-2", "Bob Jones", CoverageType.HOME, 35, "250000", "17500.00"));

        assertEquals(List.of(home), quoteRepository.findPage("home", null, 0, 10));
    }

    @Test
    void treatsWildcardCharactersAsLiterals() {
        PolicyQuote percent = quoteRepository.save(
            quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        jdbcTemplate.update("UPDATE policy_quote SET customer_number = ? WHERE quote_id = ?", "CUS_%\\1", "quote-1");
        quoteRepository.save(quote("quote-2", "Bob Jones", CoverageType.AUTO, 35, "250000", "17500.00"));

        assertEquals(1, quoteRepository.count("%", null));
        assertEquals(1, quoteRepository.count("_", null));
        assertEquals(1, quoteRepository.count("\\", null));
        assertEquals(0, quoteRepository.count("CUSX", null));
        assertEquals(percent.quoteId(), quoteRepository.findPage("S_%", null, 0, 10).get(0).quoteId());
    }

    @Test
    void pagingAndTotalsReflectTheFilter() {
        for (int index = 1; index <= 5; index++) {
            quoteRepository.save(quote("auto-" + index, "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        }
        quoteRepository.save(quote("home-1", "Bob Jones", CoverageType.HOME, 35, "250000", "17500.00"));

        assertEquals(5, quoteRepository.count("auto", null));
        List<PolicyQuote> secondPage = quoteRepository.findPage("auto", null, 2, 2);
        assertEquals(2, secondPage.size());
        assertTrue(secondPage.stream().allMatch(found -> found.coverageType() == CoverageType.AUTO));
        assertEquals(1, quoteRepository.findPage("auto", null, 4, 2).size());
    }

    @Test
    void enforcesPrimaryKeyAndDomainConstraints() {
        PolicyQuote quote = quote("duplicate-id", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00");
        quoteRepository.save(quote);

        assertThrows(DataIntegrityViolationException.class, () -> quoteRepository.save(quote));
        assertThrows(DataIntegrityViolationException.class, () ->
            jdbcTemplate.update("""
                INSERT INTO policy_quote (
                    quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium,
                    policy_term_years, payment_frequency, deductible, start_date, tax_rate, tax_amount, created_at
                ) VALUES (?, ?, ?, ?, ?, ?, 1, 'YEARLY', 0, CURRENT_DATE, 0.18, 0, now())
                """, "invalid-age", "Casey Smith", "AUTO", 17, BigDecimal.TEN, BigDecimal.ONE));
    }

    @Test
    void roundTripsPolicyDetailsAndPremiumBreakdown() {
        PolicyQuote unsaved = new PolicyQuote(
            "quote-details", "Alice Smith", CoverageType.HOME, 35,
            new BigDecimal("250000"), new BigDecimal("12500.00"), null, null, null,
            12, PaymentFrequency.QUARTERLY, new BigDecimal("1500.50"), LocalDate.of(2027, 1, 15),
            new BigDecimal("0.1800"), new BigDecimal("2250.00"),
            Instant.parse("2026-10-26T10:15:30.123456Z"), "alice");

        PolicyQuote saved = quoteRepository.save(unsaved);
        PolicyQuote found = quoteRepository.findById("quote-details").orElseThrow();

        assertEquals(unsaved.withQuoteNumber(saved.quoteNumber()), saved);
        assertEquals(saved, found);
        assertEquals(12, found.policyTermYears());
        assertEquals(PaymentFrequency.QUARTERLY, found.paymentFrequency());
        assertEquals(new BigDecimal("1500.50"), found.deductible());
        assertEquals(LocalDate.of(2027, 1, 15), found.startDate());
        assertEquals(new BigDecimal("0.1800"), found.taxRate());
        assertEquals(new BigDecimal("2250.00"), found.taxAmount());
        assertEquals(Instant.parse("2026-10-26T10:15:30.123456Z"), found.createdAt());
        assertEquals("alice", found.createdBy());
        assertEquals(new BigDecimal("14750.00"), found.totalAnnualPayable());
    }

    @Test
    void storesNullCreatedBy() {
        quoteRepository.save(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        assertEquals(null, quoteRepository.findById("quote-1").orElseThrow().createdBy());
    }

    @Test
    void searchesByCreatedByCaseInsensitively() {
        PolicyQuote byAlice = quoteRepository.save(withCreatedBy(
            quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"), "Agent.Alice"));
        quoteRepository.save(withCreatedBy(
            quote("quote-2", "Bob Jones", CoverageType.AUTO, 35, "250000", "17500.00"), "agent.bob"));
        quoteRepository.save(quote("quote-3", "Carol White", CoverageType.AUTO, 35, "250000", "17500.00"));

        assertEquals(List.of(byAlice), quoteRepository.findPage("ALICE", null, 0, 10));
        assertEquals(1, quoteRepository.count("agent.alice", null));
        assertEquals(2, quoteRepository.count("agent.", null));
    }

    @Test
    void searchesByPaymentFrequency() {
        quoteRepository.save(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        PolicyQuote monthly = quoteRepository.save(new PolicyQuote(
            "quote-2", "Bob Jones", CoverageType.AUTO, 35, new BigDecimal("250000"), new BigDecimal("17500.00"),
            null, null, null, 1, PaymentFrequency.MONTHLY, new BigDecimal("0.00"), LocalDate.of(2026, 10, 26),
            new BigDecimal("0.1800"), new BigDecimal("3150.00"), Instant.parse("2026-10-26T10:15:30Z"), null));

        assertEquals(List.of(monthly), quoteRepository.findPage("month", null, 0, 10));
        assertEquals(1, quoteRepository.count("MONTH", null));
        assertEquals(1, quoteRepository.count("yearly", null));
    }

    @Test
    void treatsWildcardCharactersInCreatedByAsLiterals() {
        quoteRepository.save(withCreatedBy(
            quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"), "a_b%c"));
        quoteRepository.save(withCreatedBy(
            quote("quote-2", "Bob Jones", CoverageType.AUTO, 35, "250000", "17500.00"), "axbyc"));

        assertEquals(1, quoteRepository.count("a_b", null));
        assertEquals(1, quoteRepository.count("b%c", null));
    }

    @Test
    void roundTripsTypeDetailsKeepingNumbersAndBooleans() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("members", 4);
        details.put("preExistingConditions", false);
        details.put("smoker", true);
        details.put("planTier", "STANDARD");
        PolicyQuote saved = quoteRepository.save(withTypeDetails(
            quote("quote-health", "Alice Smith", CoverageType.HEALTH, 35, "250000", "20000.00"), details));

        PolicyQuote found = quoteRepository.findById("quote-health").orElseThrow();

        assertEquals(saved, found);
        assertEquals(details, found.typeDetails());
        assertEquals(Integer.valueOf(4), found.typeDetails().get("members"));
        assertEquals(Boolean.FALSE, found.typeDetails().get("preExistingConditions"));
        assertEquals(Boolean.TRUE, found.typeDetails().get("smoker"));
        assertEquals("number", jdbcTemplate.queryForObject(
            "SELECT jsonb_typeof(type_details -> 'members') FROM policy_quote WHERE quote_id = 'quote-health'",
            String.class));
        assertEquals("boolean", jdbcTemplate.queryForObject(
            "SELECT jsonb_typeof(type_details -> 'smoker') FROM policy_quote WHERE quote_id = 'quote-health'",
            String.class));
    }

    @Test
    void quotesWithoutTypeDetailsReadBackAsNull() {
        quoteRepository.save(quote("quote-plain", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        jdbcTemplate.update("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium,
                policy_term_years, payment_frequency, deductible, start_date, tax_rate, tax_amount, created_at
            ) VALUES ('quote-old', 'Bob Jones', 'HOME', 40, 1000, 50, 1, 'YEARLY', 0, CURRENT_DATE, 0, 0, now())
            """);

        assertNull(quoteRepository.findById("quote-plain").orElseThrow().typeDetails());
        assertNull(quoteRepository.findById("quote-old").orElseThrow().typeDetails());
        assertNull(jdbcTemplate.queryForObject(
            "SELECT type_details::text FROM policy_quote WHERE quote_id = 'quote-plain'", String.class));
        assertTrue(quoteRepository.findPage("", null, 0, 10).stream().allMatch(q -> q.typeDetails() == null));
    }

    @Test
    void listItemsCarryTypeDetails() {
        Map<String, Object> details = Map.of("smoker", false, "occupationClass", "CLASS_2",
            "nomineeName", "Jane Doe", "nomineeRelationship", "Spouse");
        PolicyQuote saved = quoteRepository.save(withTypeDetails(
            quote("quote-life", "Alice Smith", CoverageType.LIFE, 35, "250000", "22500.00"), details));

        assertEquals(List.of(saved), quoteRepository.findPage("", null, 0, 10));
        assertEquals(details, quoteRepository.findPage("", null, 0, 10).get(0).typeDetails());
    }

    @Test
    void filtersByCoverageTypeExactlyAndCombinesWithSearch() {
        PolicyQuote autoAlice = quoteRepository.save(
            quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        quoteRepository.save(quote("quote-2", "Alice Smith", CoverageType.HOME, 35, "250000", "12500.00"));
        PolicyQuote autoBob = quoteRepository.save(
            quote("quote-3", "Bob Jones", CoverageType.AUTO, 35, "250000", "17500.00"));
        quoteRepository.save(quote("quote-4", "Bob Jones", CoverageType.LIFE, 35, "250000", "22500.00"));

        assertEquals(List.of(autoBob, autoAlice), quoteRepository.findPage("", CoverageType.AUTO, 0, 10));
        assertEquals(2, quoteRepository.count("", CoverageType.AUTO));
        assertEquals(List.of(autoAlice), quoteRepository.findPage("alice", CoverageType.AUTO, 0, 10));
        assertEquals(1, quoteRepository.count("alice", CoverageType.AUTO));
        assertEquals(0, quoteRepository.count("bob", CoverageType.HOME));
        assertEquals(4, quoteRepository.count("", null));
        assertEquals(1, quoteRepository.count("bob", CoverageType.LIFE));
    }

    @Test
    void coverageTypeFilterAppliesToEveryAlternativeOfTheSearch() {
        quoteRepository.save(quote("quote-1", "Home Owner", CoverageType.AUTO, 35, "250000", "17500.00"));
        PolicyQuote home = quoteRepository.save(
            quote("quote-2", "Home Owner", CoverageType.HOME, 35, "250000", "12500.00"));

        assertEquals(List.of(home), quoteRepository.findPage("home", CoverageType.HOME, 0, 10));
        assertEquals(1, quoteRepository.count("home", CoverageType.HOME));
        assertEquals(0, quoteRepository.count("home", CoverageType.LIFE));
    }

    @Test
    void coverageTypeFilterPaginatesWithMatchingTotals() {
        for (int i = 1; i <= 5; i++) {
            quoteRepository.save(quote("auto-" + i, "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));
        }
        quoteRepository.save(quote("home-1", "Alice Smith", CoverageType.HOME, 35, "250000", "12500.00"));

        assertEquals(5, quoteRepository.count("", CoverageType.AUTO));
        assertEquals(2, quoteRepository.findPage("", CoverageType.AUTO, 2, 2).size());
        assertEquals(1, quoteRepository.findPage("", CoverageType.AUTO, 4, 2).size());
        assertTrue(quoteRepository.findPage("", CoverageType.AUTO, 0, 10).stream()
            .allMatch(q -> q.coverageType() == CoverageType.AUTO));
    }
}
