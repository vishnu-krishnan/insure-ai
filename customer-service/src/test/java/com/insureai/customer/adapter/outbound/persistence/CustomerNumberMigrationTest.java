package com.insureai.customer.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomerNumberMigrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
        .withDatabaseName("customer_migration_db")
        .withUsername("customer_migration_admin")
        .withPassword("test-only-customer-migration-password");

    @Test
    void backfillsExistingRowsInCustomerIdOrderAndEnforcesConstraints() throws SQLException {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .target("1")
            .load()
            .migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO customer_profile (customer_id, full_name, email, phone, date_of_birth) VALUES
                ('cccccccc-0000-4000-8000-000000000003', 'Carol Third', 'carol@example.test', '+14155550103', '1990-01-01'),
                ('aaaaaaaa-0000-4000-8000-000000000001', 'Alice First', 'alice@example.test', '+14155550101', '1990-01-01'),
                ('bbbbbbbb-0000-4000-8000-000000000002', 'Bob Second', 'bob@example.test', '+14155550102', '1990-01-01')
                """);
        }

        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            List<String> numbers = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery(
                "SELECT customer_number FROM customer_profile ORDER BY customer_id")) {
                while (rows.next()) {
                    numbers.add(rows.getString(1));
                }
            }
            assertEquals(List.of("CUS-000001", "CUS-000002", "CUS-000003"), numbers);
            assertEquals(3, new HashSet<>(numbers).size());

            try (ResultSet column = statement.executeQuery("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_name = 'customer_profile' AND column_name = 'customer_number'
                """)) {
                column.next();
                assertEquals("NO", column.getString(1));
            }

            statement.executeUpdate("""
                INSERT INTO customer_profile (customer_id, full_name, email, phone, date_of_birth)
                VALUES ('dddddddd-0000-4000-8000-000000000004', 'Dan Fourth', 'dan@example.test', '+14155550104', '1990-01-01')
                """);
            try (ResultSet row = statement.executeQuery(
                "SELECT customer_number FROM customer_profile WHERE full_name = 'Dan Fourth'")) {
                row.next();
                assertEquals("CUS-000004", row.getString(1));
            }

            assertThrows(SQLException.class, () -> statement.executeUpdate(
                "UPDATE customer_profile SET customer_number = 'CUS-000001' WHERE full_name = 'Dan Fourth'"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                "UPDATE customer_profile SET customer_number = 'BAD-1' WHERE full_name = 'Dan Fourth'"));
            assertThrows(SQLException.class, () -> statement.executeUpdate(
                "UPDATE customer_profile SET customer_number = NULL WHERE full_name = 'Dan Fourth'"));
        }
    }

    @Test
    void numbersGrowBeyondSixDigitsWithoutTruncation() throws SQLException {
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
            statement.execute("SELECT setval('customer_number_seq', 999999)");
            Set<String> numbers = new HashSet<>();
            for (int i = 1; i <= 2; i++) {
                statement.executeUpdate("INSERT INTO customer_profile (customer_id, full_name, email, phone, date_of_birth) "
                    + "VALUES ('eeeeeeee-0000-4000-8000-00000000000" + i + "', 'Big Number', 'big" + i + "@example.test', '+1415555019" + i + "', '1990-01-01')");
            }
            try (ResultSet rows = statement.executeQuery("SELECT customer_number FROM customer_profile")) {
                while (rows.next()) {
                    numbers.add(rows.getString(1));
                }
            }
            assertEquals(Set.of("CUS-1000000", "CUS-1000001"), numbers);
        }
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
