package com.insureai.insurance.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

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
class QuoteNumberMigrationTest {
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
    void backfillsExistingQuotesWithDistinctNumbersInQuoteIdOrder() {
        Flyway.configure().dataSource(dataSource).target("2").load().migrate();
        insertLegacyQuote("c-quote", "Carol White", null);
        insertLegacyQuote("a-quote", "Alice Smith", "10000001");
        insertLegacyQuote("b-quote", "Bob Jones", null);

        Flyway.configure().dataSource(dataSource).target("3").load().migrate();

        assertEquals(
            List.of("QT-000001", "QT-000002", "QT-000003"),
            jdbcTemplate.queryForList("SELECT quote_number FROM policy_quote ORDER BY quote_id", String.class)
        );
        assertEquals(0, jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM policy_quote WHERE customer_number IS NOT NULL", Integer.class));
        assertEquals("NO", jdbcTemplate.queryForObject("""
            SELECT is_nullable FROM information_schema.columns
            WHERE table_name = 'policy_quote' AND column_name = 'quote_number'
            """, String.class));
        assertEquals("YES", jdbcTemplate.queryForObject("""
            SELECT is_nullable FROM information_schema.columns
            WHERE table_name = 'policy_quote' AND column_name = 'customer_number'
            """, String.class));

        String generated = jdbcTemplate.queryForObject("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium
            ) VALUES ('d-quote', 'Dana Lee', 'AUTO', 30, 1000, 70)
            RETURNING quote_number
            """, String.class);
        assertEquals("QT-000004", generated);
        assertNull(jdbcTemplate.queryForObject(
            "SELECT customer_number FROM policy_quote WHERE quote_id = 'd-quote'", String.class));
    }

    @Test
    void enforcesQuoteNumberUniquenessAndFormat() {
        Flyway.configure().dataSource(dataSource).target("3").load().migrate();
        jdbcTemplate.update("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium, quote_number
            ) VALUES ('first', 'Alice Smith', 'AUTO', 30, 1000, 70, 'QT-900001')
            """);

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium, quote_number
            ) VALUES ('second', 'Bob Jones', 'AUTO', 30, 1000, 70, 'QT-900001')
            """));
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium, quote_number
            ) VALUES ('third', 'Carol White', 'AUTO', 30, 1000, 70, 'QT-12')
            """));
    }

    private void insertLegacyQuote(String quoteId, String name, String customerId) {
        jdbcTemplate.update("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium, customer_id
            ) VALUES (?, ?, 'AUTO', 35, ?, ?, ?)
            """, quoteId, name, new BigDecimal("250000"), new BigDecimal("17500.00"), customerId);
    }
}
