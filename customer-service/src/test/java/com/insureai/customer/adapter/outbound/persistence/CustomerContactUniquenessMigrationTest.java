package com.insureai.customer.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomerContactUniquenessMigrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
        .withDatabaseName("customer_uniqueness_migration_db")
        .withUsername("customer_uniqueness_admin")
        .withPassword("test-only-customer-uniqueness-password");

    @Test
    void failsLoudlyWhenExistingRowsHoldDuplicateContacts() throws SQLException {
        resetTo("3");
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO customer_profile (customer_id, full_name, email, phone, date_of_birth) VALUES
                ('aaaaaaaa-0000-4000-8000-000000000001', 'Alice First', 'alice@example.test', '+14155550101', '1990-01-01'),
                ('bbbbbbbb-0000-4000-8000-000000000002', 'Alice Second', 'ALICE@example.test', '+14155550102', '1991-02-02')
                """);
        }

        assertThrows(FlywayException.class, () -> flyway(null).migrate());
        try (Connection connection = connection(); Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM pg_indexes WHERE indexname IN ('customer_profile_email_unique', 'customer_profile_phone_unique')")) {
            assertTrue(rows.next());
            assertEquals(0, rows.getInt(1));
        }
    }

    @Test
    void enforcesUniqueEmailIgnoringCaseAndUniquePhone() throws SQLException {
        resetTo(null);
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            insert(statement, "alice@example.test", "+14155550101");
            assertThrows(SQLException.class, () -> insert(statement, "Alice@Example.test", "+14155550102"));
            assertThrows(SQLException.class, () -> insert(statement, "other@example.test", "+14155550101"));
            insert(statement, "other@example.test", "+14155550102");
        }
    }

    private static void resetTo(String target) {
        flyway(null).clean();
        flyway(target).migrate();
    }

    private static Flyway flyway(String target) {
        return Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .cleanDisabled(false)
            .target(target == null ? "latest" : target)
            .load();
    }

    private static void insert(Statement statement, String email, String phone) throws SQLException {
        statement.executeUpdate("INSERT INTO customer_profile (customer_id, full_name, email, phone, date_of_birth) VALUES ('"
            + java.util.UUID.randomUUID() + "', 'Unique Tester', '" + email + "', '" + phone + "', '1990-01-01')");
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
