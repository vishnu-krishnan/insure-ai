package com.insureai.insurance.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class QuoteDetailsMigrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
        .withDatabaseName("insure_migration_db")
        .withUsername("insure_test_admin")
        .withPassword("test-only-insure-admin-password");

    private DriverManagerDataSource dataSource;
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetSchema() {
        dataSource = new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute("DROP SCHEMA public CASCADE");
        jdbcTemplate.execute("CREATE SCHEMA public");
    }

    @Test
    void backfillsLegacyQuotesWithDefaults() {
        Flyway.configure().dataSource(dataSource).target("3").load().migrate();
        insertLegacyQuote("a-quote");
        insertLegacyQuote("b-quote");

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertEquals(2, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM policy_quote", Integer.class));
        for (Map<String, Object> row : jdbcTemplate.queryForList("SELECT * FROM policy_quote")) {
            assertEquals(1, ((Number) row.get("policy_term_years")).intValue());
            assertEquals("YEARLY", row.get("payment_frequency"));
            assertEquals(0, new BigDecimal("0").compareTo((BigDecimal) row.get("deductible")));
            assertEquals(0, new BigDecimal("0").compareTo((BigDecimal) row.get("tax_rate")));
            assertEquals(0, new BigDecimal("0").compareTo((BigDecimal) row.get("tax_amount")));
            assertNull(row.get("created_by"));
        }
        assertEquals(2, jdbcTemplate.queryForObject("""
            SELECT COUNT(*) FROM policy_quote
            WHERE start_date = CURRENT_DATE AND created_at IS NOT NULL AND created_at <= now()
            """, Integer.class));
    }

    @Test
    void requiresValuesForNewColumnsExceptCreatedBy() {
        Flyway.configure().dataSource(dataSource).load().migrate();

        for (String column : new String[] {
            "policy_term_years", "payment_frequency", "deductible", "start_date", "tax_rate", "tax_amount",
            "created_at"
        }) {
            assertEquals("NO", nullable(column), column);
        }
        assertEquals("YES", nullable("created_by"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium
            ) VALUES ('no-details', 'Dana Lee', 'AUTO', 30, 1000, 70)
            """));
    }

    @Test
    void constraintsRejectInvalidValues() {
        Flyway.configure().dataSource(dataSource).load().migrate();

        insert("ok-1", 1, "YEARLY", "0", "0", "0");
        insert("ok-40", 40, "MONTHLY", "0.00", "1", "0");
        insert("ok-q", 5, "QUARTERLY", "999999999999.99", "0.1800", "12.50");

        assertRejected("term-0", 0, "YEARLY", "0", "0.18", "0");
        assertRejected("term-41", 41, "YEARLY", "0", "0.18", "0");
        assertRejected("freq", 1, "WEEKLY", "0", "0.18", "0");
        assertRejected("deductible", 1, "YEARLY", "-0.01", "0.18", "0");
        assertRejected("rate-low", 1, "YEARLY", "0", "-0.0001", "0");
        assertRejected("rate-high", 1, "YEARLY", "0", "1.0001", "0");
        assertRejected("tax", 1, "YEARLY", "0", "0.18", "-0.01");
    }

    private String nullable(String column) {
        return jdbcTemplate.queryForObject("""
            SELECT is_nullable FROM information_schema.columns
            WHERE table_name = 'policy_quote' AND column_name = ?
            """, String.class, column);
    }

    private void assertRejected(String id, int term, String frequency, String deductible, String rate, String tax) {
        assertThrows(DataIntegrityViolationException.class, () -> insert(id, term, frequency, deductible, rate, tax), id);
    }

    private void insert(String id, int term, String frequency, String deductible, String rate, String tax) {
        jdbcTemplate.update("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium,
                policy_term_years, payment_frequency, deductible, start_date, tax_rate, tax_amount, created_at
            ) VALUES (?, 'Dana Lee', 'AUTO', 30, 1000, 70, ?, ?, ?, CURRENT_DATE, ?, ?, now())
            """, id, term, frequency, new BigDecimal(deductible), new BigDecimal(rate), new BigDecimal(tax));
    }

    private void insertLegacyQuote(String quoteId) {
        jdbcTemplate.update("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium
            ) VALUES (?, 'Alice Smith', 'AUTO', 35, 250000, 17500.00)
            """, quoteId);
    }
}
