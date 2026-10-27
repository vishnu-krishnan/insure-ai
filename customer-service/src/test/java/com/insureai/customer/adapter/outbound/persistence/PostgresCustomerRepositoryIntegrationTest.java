package com.insureai.customer.adapter.outbound.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
import com.insureai.customer.domain.model.Address;
import com.insureai.customer.domain.model.CustomerProfile;
import com.insureai.customer.domain.model.Gender;
import com.insureai.customer.domain.model.MaritalStatus;
import com.insureai.customer.domain.model.Nominee;

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
        assertEquals("3", flyway.info().current().getVersion().getVersion());
        CustomerProfile unsaved = profile("3b241101-e2bb-4255-8caf-4136c566a962", "Alice Smith", "alice@example.test", "+14155552671");

        CustomerProfile saved = customerRepository.save(unsaved);

        assertTrue(saved.customerNumber().matches("CUS-[0-9]{6,}"));
        assertEquals(unsaved.customerId(), saved.customerId());
        assertEquals(unsaved.fullName(), saved.fullName());
        assertEquals(Optional.of(saved), customerRepository.findById(unsaved.customerId()));
    }

    @Test
    void assignsIncreasingCustomerNumbers() {
        CustomerProfile first = customerRepository.save(profile(UUID.randomUUID().toString(), "Seq One", "seq1@example.test", "+14155550001"));
        CustomerProfile second = customerRepository.save(profile(UUID.randomUUID().toString(), "Seq Two", "seq2@example.test", "+14155550002"));

        assertTrue(number(second) > number(first));
    }

    @Test
    void searchesByNameFragmentIgnoringCase() {
        CustomerProfile saved = customerRepository.save(profile(UUID.randomUUID().toString(), "Zephyrine Quillfeather", "zq@example.test", "+14155550003"));

        assertEquals(List.of(saved), customerRepository.search("phyrINE quill", 0, 10));
        assertEquals(List.of(saved), customerRepository.search("PHYRINE", 0, 10));
        assertEquals(1, customerRepository.countSearch("quillFEATHER"));
    }

    @Test
    void searchesByCustomerNumber() {
        CustomerProfile saved = customerRepository.save(profile(UUID.randomUUID().toString(), "Number Lookup", "nl@example.test", "+14155550004"));

        assertEquals(List.of(saved), customerRepository.search(saved.customerNumber(), 0, 10));
        assertEquals(List.of(saved), customerRepository.search(saved.customerNumber().toLowerCase(), 0, 10));
    }

    @Test
    void searchesByEmail() {
        CustomerProfile saved = customerRepository.save(profile(UUID.randomUUID().toString(), "Email Lookup", "unique.mailbox.zz@example.test", "+14155550005"));

        assertEquals(List.of(saved), customerRepository.search("MAILBOX.ZZ@", 0, 10));
    }

    @Test
    void searchesByPhone() {
        CustomerProfile saved = customerRepository.save(profile(UUID.randomUUID().toString(), "Phone Lookup", "pl@example.test", "+447700900123"));

        assertEquals(List.of(saved), customerRepository.search("7700900123", 0, 10));
    }

    @Test
    void searchesByFullUuidAndUuidPrefix() {
        String id = "9f8e7d6c-1111-4222-8333-444455556666";
        CustomerProfile saved = customerRepository.save(profile(id, "Uuid Lookup", "ul@example.test", "+14155550006"));

        assertEquals(List.of(saved), customerRepository.search(id, 0, 10));
        assertEquals(List.of(saved), customerRepository.search("9F8E7D6C-1111", 0, 10));
    }

    @Test
    void percentAndUnderscoreAreMatchedLiterally() {
        CustomerProfile percent = customerRepository.save(profile(UUID.randomUUID().toString(), "Wild 100% Card", "wild1@example.test", "+14155550007"));
        CustomerProfile underscore = customerRepository.save(profile(UUID.randomUUID().toString(), "Wild_Card Under", "wild2@example.test", "+14155550008"));
        customerRepository.save(profile(UUID.randomUUID().toString(), "Wild 100X Card", "wild3@example.test", "+14155550009"));
        customerRepository.save(profile(UUID.randomUUID().toString(), "WildXCard Under", "wild4@example.test", "+14155550010"));

        assertEquals(List.of(percent), customerRepository.search("100%", 0, 10));
        assertEquals(List.of(underscore), customerRepository.search("wild_card", 0, 10));
        assertEquals(0, customerRepository.countSearch("%zzz%"));
        assertEquals(0, customerRepository.countSearch("\\"));
        assertEquals(0, customerRepository.countSearch("x' OR '1'='1"));
    }

    @Test
    void pagesNewestFirstWithTotals() {
        List<CustomerProfile> saved = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            saved.add(customerRepository.save(profile(UUID.randomUUID().toString(), "Pager Person" + i, "pager" + i + "@example.test", "+1415555100" + i)));
        }

        List<CustomerProfile> firstPage = customerRepository.search("pager person", 0, 2);
        List<CustomerProfile> secondPage = customerRepository.search("pager person", 2, 2);
        List<CustomerProfile> lastPage = customerRepository.search("pager person", 4, 2);

        assertEquals(5, customerRepository.countSearch("pager person"));
        assertEquals(List.of(saved.get(4), saved.get(3)), firstPage);
        assertEquals(List.of(saved.get(2), saved.get(1)), secondPage);
        assertEquals(List.of(saved.get(0)), lastPage);
    }

    @Test
    void emptyTextMatchesEveryCustomer() {
        customerRepository.save(profile(UUID.randomUUID().toString(), "Anyone Atall", "anyone@example.test", "+14155550011"));

        assertTrue(customerRepository.countSearch("") >= 1);
        assertFalse(customerRepository.search("", 0, 10).isEmpty());
    }

    @Test
    void roundTripsAllCustomerDetails() {
        CustomerProfile unsaved = detailed(UUID.randomUUID().toString(), "Detail Roundtrip", "dr@example.test", "+14155550020",
            "Cartographer", new Address("1 Main St", "Flat 2", "Springfield", "Illinois", "62701-1234", "US"));

        CustomerProfile saved = customerRepository.save(unsaved);

        assertNotNull(saved.createdAt());
        assertEquals(saved.createdAt(), saved.updatedAt());
        assertEquals(Gender.FEMALE, saved.gender());
        assertEquals(new BigDecimal("1234567.50"), saved.annualIncome());
        assertEquals(unsaved.address(), saved.address());
        assertEquals(unsaved.nominee(), saved.nominee());
        assertEquals(Optional.of(saved), customerRepository.findById(unsaved.customerId()));
    }

    @Test
    void roundTripsMissingAddressAndNominee() {
        CustomerProfile saved = customerRepository.save(profile(UUID.randomUUID().toString(), "No Details", "nd@example.test", "+14155550021"));

        CustomerProfile loaded = customerRepository.findById(saved.customerId()).orElseThrow();
        assertNull(loaded.address());
        assertNull(loaded.nominee());
        assertNull(loaded.gender());
        assertNull(loaded.annualIncome());
        assertNotNull(loaded.createdAt());
        assertEquals(saved, loaded);
    }

    @Test
    void roundTripsAddressWithoutLineTwo() {
        CustomerProfile saved = customerRepository.save(detailed(UUID.randomUUID().toString(), "Single Line", "sl@example.test", "+14155550022",
            null, new Address("9 Side Rd", null, "Leeds", "West Yorkshire", "LS1 4AP", "GB")));

        assertNull(customerRepository.findById(saved.customerId()).orElseThrow().address().line2());
    }

    @Test
    void searchesByOccupationCityAndPostalCode() {
        CustomerProfile saved = customerRepository.save(detailed(UUID.randomUUID().toString(), "Detail Search", "ds@example.test", "+14155550023",
            "Lighthouse Keeper", new Address("5 Harbor Way", null, "Zanzibarville", "Coast", "QX9-77Z", "US")));

        assertEquals(List.of(saved), customerRepository.search("lighthouse KEEP", 0, 10));
        assertEquals(List.of(saved), customerRepository.search("zanzibar", 0, 10));
        assertEquals(List.of(saved), customerRepository.search("qx9-77", 0, 10));
        assertEquals(1, customerRepository.countSearch("QX9-77Z"));
        assertEquals(0, customerRepository.countSearch("Harbor Way"));
    }

    private static CustomerProfile detailed(String id, String name, String email, String phone, String occupation, Address address) {
        return new CustomerProfile(id, null, name, email, phone, LocalDate.of(1990, 1, 1),
            Gender.FEMALE, MaritalStatus.DIVORCED, occupation, new BigDecimal("1234567.5"),
            address, new Nominee("Pat Doe", "Sibling"), null, null);
    }

    private static CustomerProfile profile(String id, String name, String email, String phone) {
        return new CustomerProfile(id, null, name, email, phone, LocalDate.of(1990, 1, 1), null, null, null, null, null, null, null, null);
    }

    private static long number(CustomerProfile customer) {
        return Long.parseLong(customer.customerNumber().substring(4));
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
