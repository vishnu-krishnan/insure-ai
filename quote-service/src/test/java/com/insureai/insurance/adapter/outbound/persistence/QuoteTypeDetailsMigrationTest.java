package com.insureai.insurance.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
class QuoteTypeDetailsMigrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
        .withDatabaseName("insure_type_details_db")
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
    void addsNullableJsonbColumnAndLeavesExistingRowsNull() {
        Flyway.configure().dataSource(dataSource).target("4").load().migrate();
        jdbcTemplate.update("""
            INSERT INTO policy_quote (
                quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium,
                policy_term_years, payment_frequency, deductible, start_date, tax_rate, tax_amount, created_at
            ) VALUES ('old-quote', 'Alice Smith', 'AUTO', 35, 250000, 17500.00,
                1, 'YEARLY', 0, CURRENT_DATE, 0.18, 0, now())
            """);

        Flyway.configure().dataSource(dataSource).load().migrate();

        assertEquals("jsonb", jdbcTemplate.queryForObject("""
            SELECT data_type FROM information_schema.columns
            WHERE table_name = 'policy_quote' AND column_name = 'type_details'
            """, String.class));
        assertEquals("YES", jdbcTemplate.queryForObject("""
            SELECT is_nullable FROM information_schema.columns
            WHERE table_name = 'policy_quote' AND column_name = 'type_details'
            """, String.class));
        assertNull(jdbcTemplate.queryForObject(
            "SELECT type_details::text FROM policy_quote WHERE quote_id = 'old-quote'", String.class));
    }
}
