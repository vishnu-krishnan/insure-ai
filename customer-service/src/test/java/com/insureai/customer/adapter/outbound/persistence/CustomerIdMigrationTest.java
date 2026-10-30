package com.insureai.customer.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomerIdMigrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
        .withDatabaseName("customer_id_migration_db")
        .withUsername("customer_migration_admin")
        .withPassword("test-only-customer-migration-password");

    @Test
    void convertsUuidIdsFromCustomerNumbersAndContinuesTheSequence() throws SQLException {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .cleanDisabled(false)
            .load()
            .clean();
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .target("4")
            .load()
            .migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO customer_profile (customer_id, full_name, email, phone, date_of_birth) VALUES
                ('aaaaaaaa-0000-4000-8000-000000000001', 'Alice First', 'alice@example.test', '+14155550101', '1990-01-01'),
                ('bbbbbbbb-0000-4000-8000-000000000002', 'Bob Second', 'bob@example.test', '+14155550102', '1990-01-01')
                """);
        }

        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            Map<String, String> idsByNumber = new LinkedHashMap<>();
            try (ResultSet rows = statement.executeQuery(
                "SELECT customer_number, customer_id FROM customer_profile ORDER BY customer_number")) {
                while (rows.next()) {
                    idsByNumber.put(rows.getString(1), rows.getString(2));
                }
            }
            assertEquals(Map.of("CUS-000001", "10000001", "CUS-000002", "10000002"), idsByNumber);

            statement.executeUpdate("""
                INSERT INTO customer_profile (full_name, email, phone, date_of_birth)
                VALUES ('Carol Third', 'carol@example.test', '+14155550103', '1990-01-01')
                """);
            try (ResultSet row = statement.executeQuery(
                "SELECT customer_id FROM customer_profile WHERE full_name = 'Carol Third'")) {
                row.next();
                assertEquals("10000003", row.getString(1));
            }

            assertThrows(SQLException.class, () -> statement.executeUpdate(
                "UPDATE customer_profile SET customer_id = '3b241101-e2bb-4255-8caf-4136c566a962' WHERE full_name = 'Carol Third'"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                "UPDATE customer_profile SET customer_id = '123' WHERE full_name = 'Carol Third'"));
        }
    }

    @Test
    void startsAtTenMillionOneWhenThereAreNoCustomers() throws SQLException {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .cleanDisabled(false)
            .load()
            .clean();
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO customer_profile (full_name, email, phone, date_of_birth)
                VALUES ('First Ever', 'first@example.test', '+14155550201', '1990-01-01')
                """);
            try (ResultSet row = statement.executeQuery("SELECT customer_id FROM customer_profile")) {
                row.next();
                assertEquals("10000001", row.getString(1));
            }
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
