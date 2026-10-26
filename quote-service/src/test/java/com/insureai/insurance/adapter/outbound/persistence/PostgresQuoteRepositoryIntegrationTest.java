package com.insureai.insurance.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
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
        assertEquals("2", flyway.info().current().getVersion().getVersion());
        assertEquals(1, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = 'policy_quote'",
            Integer.class
        ));
    }

    @Test
    void savesAndRetrievesQuoteWithDomainFieldsMapped() {
        PolicyQuote expected = quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00");

        quoteRepository.save(expected);

        assertEquals(List.of(expected), quoteRepository.findPage(0, 10));
    }

    @Test
    void findsQuoteByIdAndReturnsEmptyForUnknownId() {
        PolicyQuote expected = quote("quote-by-id", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00");
        quoteRepository.save(expected);

        assertEquals(Optional.of(expected), quoteRepository.findById("quote-by-id"));
        assertEquals(Optional.empty(), quoteRepository.findById("unknown"));
    }

    @Test
    void pagesQuotesInStableOrderAndCountsThem() {
        assertTrue(quoteRepository.findPage(0, 10).isEmpty());
        assertEquals(0, quoteRepository.count());
        PolicyQuote first = quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00");
        PolicyQuote second = quote("quote-2", "Bob Jones", CoverageType.HOME, 65, "400000", "23000.00");
        PolicyQuote third = quote("quote-3", "Carol White", CoverageType.LIFE, 40, "300000", "27000.00");

        quoteRepository.save(third);
        quoteRepository.save(first);
        quoteRepository.save(second);

        assertEquals(3, quoteRepository.count());
        assertEquals(List.of(first, second), quoteRepository.findPage(0, 2));
        assertEquals(List.of(third), quoteRepository.findPage(2, 2));
        assertTrue(quoteRepository.findPage(4, 2).isEmpty());
    }

    @Test
    void enforcesPrimaryKeyAndDomainConstraints() {
        PolicyQuote quote = quote("duplicate-id", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00");
        quoteRepository.save(quote);

        assertThrows(DataIntegrityViolationException.class, () -> quoteRepository.save(quote));
        assertThrows(DataIntegrityViolationException.class, () ->
            jdbcTemplate.update("""
                INSERT INTO policy_quote (
                    quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium
                ) VALUES (?, ?, ?, ?, ?, ?)
                """, "invalid-age", "Casey Smith", "AUTO", 17, BigDecimal.TEN, BigDecimal.ONE));
    }

    private static PolicyQuote quote(
        String id,
        String customerName,
        CoverageType coverageType,
        int age,
        String amount,
        String annualPremium
    ) {
        return new PolicyQuote(
            id,
            customerName,
            coverageType,
            age,
            new BigDecimal(amount),
            new BigDecimal(annualPremium)
        );
    }
}
