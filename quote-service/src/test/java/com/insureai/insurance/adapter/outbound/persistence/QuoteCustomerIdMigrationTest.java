package com.insureai.insurance.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class QuoteCustomerIdMigrationTest {
    private static final String LEGACY_UUID = "3b241101-e2bb-4255-8caf-4136c566a962";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
        .withDatabaseName("insure_customer_id_migration_db")
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
    void mapsStoredUuidReferencesFromCustomerNumbersAndKeepsUnresolvableOnes() {
        Flyway.configure().dataSource(dataSource).target("3").load().migrate();
        insertQuote("mapped", LEGACY_UUID, "CUS-000008");
        insertQuote("large", "6b241101-e2bb-4255-8caf-4136c566a962", "CUS-1000000");
        insertQuote("no-number", "9b241101-e2bb-4255-8caf-4136c566a962", null);
        insertQuote("no-customer", null, null);
        insertQuote("already-numeric", "10000042", "CUS-000042");

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertEquals("10000008", customerId("mapped"));
        assertEquals("11000000", customerId("large"));
        assertEquals("9b241101-e2bb-4255-8caf-4136c566a962", customerId("no-number"));
        assertNull(customerId("no-customer"));
        assertEquals("10000042", customerId("already-numeric"));
    }

    private String customerId(String quoteId) {
        return jdbcTemplate.queryForObject(
            "SELECT customer_id FROM policy_quote WHERE quote_id = ?", String.class, quoteId);
    }

    private void insertQuote(String quoteId, String customerId, String customerNumber) {
        jdbcTemplate.update("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium,
                customer_id, customer_number
            ) VALUES (?, 'Test Customer', 'AUTO', 35, ?, ?, ?, ?)
            """, quoteId, new BigDecimal("250000"), new BigDecimal("17500.00"), customerId, customerNumber);
    }
}
