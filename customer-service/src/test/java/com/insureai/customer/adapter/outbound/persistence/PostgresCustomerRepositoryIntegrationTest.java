package com.insureai.customer.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Optional;

import org.flywaydb.core.Flyway;
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

import com.insureai.customer.application.port.outbound.CustomerRepositoryPort;
import com.insureai.customer.domain.model.CustomerProfile;

@SpringBootTest(properties = {
    "security.jwt.issuer-uri=https://issuer.example/realms/test",
    "security.jwt.audience=customer-service"
})
@Testcontainers
class PostgresCustomerRepositoryIntegrationTest {
    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16")
        .withDatabaseName("customer_test_db")
        .withUsername("customer_test_admin")
        .withPassword("test-only-customer-test-admin-password")
        .withInitScript("db/test-container-init.sql");

    @Autowired
    private CustomerRepositoryPort customerRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "customer_test_app");
        registry.add("spring.datasource.password", () -> "test-only-customer-app-password");
    }

    @Test
    void appliesCustomerOwnedMigrationAndPersistsProfile() {
        assertEquals("1", flyway.info().current().getVersion().getVersion());
        CustomerProfile expected = new CustomerProfile(
            "3b241101-e2bb-4255-8caf-4136c566a962",
            "Alice Smith",
            "alice@example.test",
            "+14155552671",
            LocalDate.of(1990, 1, 1)
        );

        customerRepository.save(expected);

        assertEquals(Optional.of(expected), customerRepository.findById(expected.customerId()));
    }

    @Test
    void migrationAcceptsE164PhoneAndRejectsInvalidPhone() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
            INSERT INTO customer_profile (customer_id, full_name, email, phone, date_of_birth)
            VALUES (?, ?, ?, ?, ?)
            """,
            "3b241101-e2bb-4255-8caf-4136c566a963",
            "Bob Jones",
            "bob@example.test",
            "4155552671",
            LocalDate.of(1985, 1, 1)
        ));
    }
}
