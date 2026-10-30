package com.insureai.customer.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class CustomerDetailsMigrationTest {
    private static final List<String> DETAIL_COLUMNS = List.of(
        "gender", "marital_status", "occupation", "annual_income", "address_line1", "address_line2",
        "address_city", "address_state", "address_postal_code", "address_country",
        "nominee_name", "nominee_relationship"
    );
    private static final String VALID_ADDRESS_COLUMNS =
        "address_line1, address_city, address_state, address_postal_code, address_country";
    private static final AtomicInteger PHONE_SEQUENCE = new AtomicInteger(1000);
    private static final String VALID_ADDRESS_VALUES = "'1 Main St', 'Pune', 'MH', '411001', 'IN'";

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
        .withDatabaseName("customer_details_migration_db")
        .withUsername("customer_details_admin")
        .withPassword("test-only-customer-details-password");

    @Test
    void keepsExistingRowsAndLeavesNewColumnsNull() throws SQLException {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .cleanDisabled(false)
            .load()
            .clean();
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .target("2")
            .load()
            .migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                INSERT INTO customer_profile (customer_id, full_name, email, phone, date_of_birth) VALUES
                ('aaaaaaaa-0000-4000-8000-000000000001', 'Alice First', 'alice@example.test', '+14155550101', '1990-01-01'),
                ('bbbbbbbb-0000-4000-8000-000000000002', 'Bob Second', 'bob@example.test', '+14155550102', '1991-02-02')
                """);
        }

        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();

        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery(
                "SELECT customer_number, full_name, created_at, updated_at, "
                    + String.join(", ", DETAIL_COLUMNS) + " FROM customer_profile ORDER BY customer_id")) {
                int count = 0;
                while (rows.next()) {
                    count++;
                    assertEquals("CUS-00000" + count, rows.getString("customer_number"));
                    assertNotNull(rows.getTimestamp("created_at"));
                    assertEquals(rows.getTimestamp("created_at"), rows.getTimestamp("updated_at"));
                    for (String column : DETAIL_COLUMNS) {
                        assertNull(rows.getObject(column), column);
                    }
                    assertEquals(count == 1 ? "Alice First" : "Bob Second", rows.getString("full_name"));
                }
                assertEquals(2, count);
            }

            insert(statement, "gender, marital_status, occupation, annual_income",
                "'MALE', 'MARRIED', 'Engineer', 999999999999.99");
            insert(statement, VALID_ADDRESS_COLUMNS + ", nominee_name, nominee_relationship",
                VALID_ADDRESS_VALUES + ", 'Nina', 'Spouse'");
            try (ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM customer_profile")) {
                assertTrue(rows.next());
                assertEquals(4, rows.getInt(1));
            }
        }
    }

    @Test
    void rejectsInvalidDetails() throws SQLException {
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
            assertRejected(statement, "gender", "'ROBOT'");
            assertRejected(statement, "marital_status", "'COMPLICATED'");
            assertRejected(statement, "annual_income", "-1");
            assertRejected(statement, "annual_income", "1000000000000");
            assertRejected(statement, "address_country", "'in'");
            assertRejected(statement, "address_country", "'IND'");
            assertRejected(statement, "address_line1", "'1 Main St'");
            assertRejected(statement, "address_city", "'Pune'");
            assertRejected(statement, "address_line2", "'Flat 2'");
            assertRejected(statement, "nominee_name", "'Nina'");
            assertRejected(statement, "nominee_relationship", "'Spouse'");
            assertRejected(statement, VALID_ADDRESS_COLUMNS.replace("address_postal_code, ", ""),
                "'1 Main St', 'Pune', 'MH', 'IN'");

            insert(statement, "gender, marital_status, annual_income", "'UNSPECIFIED', 'WIDOWED', 0");
            insert(statement, VALID_ADDRESS_COLUMNS + ", address_line2", VALID_ADDRESS_VALUES + ", 'Flat 2'");
        }
    }

    private static void assertRejected(Statement statement, String columns, String values) {
        assertThrows(SQLException.class, () -> insert(statement, columns, values), columns + " = " + values);
    }

    private static void insert(Statement statement, String columns, String values) throws SQLException {
        statement.executeUpdate("INSERT INTO customer_profile (full_name, email, phone, date_of_birth, "
            + columns + ") VALUES ('Detail Tester', '" + UUID.randomUUID()
            + "@example.test', '+1415" + (5550000 + PHONE_SEQUENCE.incrementAndGet()) + "', '1990-01-01', " + values + ")");
    }

    private static Connection connection() throws SQLException {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }
}
