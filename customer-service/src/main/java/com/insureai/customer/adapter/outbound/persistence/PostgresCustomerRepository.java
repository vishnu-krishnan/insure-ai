package com.insureai.customer.adapter.outbound.persistence;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.insureai.customer.application.port.outbound.CustomerRepositoryPort;
import com.insureai.customer.domain.exception.DuplicateCustomerException;
import com.insureai.customer.domain.model.Address;
import com.insureai.customer.domain.model.CustomerProfile;
import com.insureai.customer.domain.model.Gender;
import com.insureai.customer.domain.model.MaritalStatus;
import com.insureai.customer.domain.model.Nominee;

@Repository
public class PostgresCustomerRepository implements CustomerRepositoryPort {
    private static final String INSERT_CUSTOMER = """
        INSERT INTO customer_profile (
            customer_id, full_name, email, phone, date_of_birth,
            gender, marital_status, occupation, annual_income,
            address_line1, address_line2, address_city, address_state, address_postal_code, address_country,
            nominee_name, nominee_relationship
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING customer_number, annual_income, created_at, updated_at
        """;
    private static final String EMAIL_EXISTS = "SELECT EXISTS (SELECT 1 FROM customer_profile WHERE lower(email) = lower(?))";
    private static final String PHONE_EXISTS = "SELECT EXISTS (SELECT 1 FROM customer_profile WHERE phone = ?)";
    private static final String EMAIL_UNIQUE_INDEX = "customer_profile_email_unique";
    private static final String PHONE_UNIQUE_INDEX = "customer_profile_phone_unique";
    private static final String CUSTOMER_COLUMNS = """
        customer_id, customer_number, full_name, email, phone, date_of_birth,
        gender, marital_status, occupation, annual_income,
        address_line1, address_line2, address_city, address_state, address_postal_code, address_country,
        nominee_name, nominee_relationship, created_at, updated_at
        """;
    private static final String SELECT_CUSTOMER = "SELECT " + CUSTOMER_COLUMNS + """
        FROM customer_profile
        WHERE customer_id = ?
        """;
    private static final String SEARCH_FILTER = """
        WHERE lower(full_name) LIKE ? ESCAPE '\\'
           OR lower(email) LIKE ? ESCAPE '\\'
           OR phone LIKE ? ESCAPE '\\'
           OR lower(customer_number) LIKE ? ESCAPE '\\'
           OR lower(customer_id) LIKE ? ESCAPE '\\'
           OR lower(occupation) LIKE ? ESCAPE '\\'
           OR lower(address_city) LIKE ? ESCAPE '\\'
           OR lower(address_postal_code) LIKE ? ESCAPE '\\'
        """;
    private static final String SEARCH_CUSTOMERS = "SELECT " + CUSTOMER_COLUMNS + """
        FROM customer_profile
        """ + SEARCH_FILTER + """
        ORDER BY LENGTH(customer_number) DESC, customer_number DESC
        LIMIT ? OFFSET ?
        """;
    private static final String COUNT_CUSTOMERS = "SELECT COUNT(*) FROM customer_profile " + SEARCH_FILTER;
    private static final RowMapper<CustomerProfile> CUSTOMER_ROW_MAPPER = PostgresCustomerRepository::mapCustomer;

    private final JdbcTemplate jdbcTemplate;

    public PostgresCustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CustomerProfile save(CustomerProfile customer) {
        try {
            return insert(customer);
        } catch (DuplicateKeyException exception) {
            String message = String.valueOf(exception.getMostSpecificCause().getMessage());
            if (message.contains(EMAIL_UNIQUE_INDEX)) {
                throw new DuplicateCustomerException("email");
            }
            if (message.contains(PHONE_UNIQUE_INDEX)) {
                throw new DuplicateCustomerException("phone");
            }
            throw exception;
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(EMAIL_EXISTS, Boolean.class, email));
    }

    @Override
    public boolean existsByPhone(String phone) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(PHONE_EXISTS, Boolean.class, phone));
    }

    private CustomerProfile insert(CustomerProfile customer) {
        Address address = customer.address();
        Nominee nominee = customer.nominee();
        return jdbcTemplate.queryForObject(
            INSERT_CUSTOMER,
            (resultSet, rowNumber) -> new CustomerProfile(
                customer.customerId(),
                resultSet.getString("customer_number"),
                customer.fullName(),
                customer.email(),
                customer.phone(),
                customer.dateOfBirth(),
                customer.gender(),
                customer.maritalStatus(),
                customer.occupation(),
                resultSet.getBigDecimal("annual_income"),
                customer.address(),
                customer.nominee(),
                instant(resultSet, "created_at"),
                instant(resultSet, "updated_at")
            ),
            customer.customerId(),
            customer.fullName(),
            customer.email(),
            customer.phone(),
            customer.dateOfBirth(),
            customer.gender() == null ? null : customer.gender().name(),
            customer.maritalStatus() == null ? null : customer.maritalStatus().name(),
            customer.occupation(),
            customer.annualIncome(),
            address == null ? null : address.line1(),
            address == null ? null : address.line2(),
            address == null ? null : address.city(),
            address == null ? null : address.state(),
            address == null ? null : address.postalCode(),
            address == null ? null : address.country(),
            nominee == null ? null : nominee.name(),
            nominee == null ? null : nominee.relationship()
        );
    }

    @Override
    public Optional<CustomerProfile> findById(String customerId) {
        List<CustomerProfile> customers = jdbcTemplate.query(SELECT_CUSTOMER, CUSTOMER_ROW_MAPPER, customerId);
        return customers.stream().findFirst();
    }

    @Override
    public List<CustomerProfile> search(String text, long offset, int limit) {
        String pattern = likePattern(text);
        return jdbcTemplate.query(
            SEARCH_CUSTOMERS,
            CUSTOMER_ROW_MAPPER,
            pattern, pattern, pattern, pattern, pattern, pattern, pattern, pattern,
            limit,
            offset
        );
    }

    @Override
    public long countSearch(String text) {
        String pattern = likePattern(text);
        Long total = jdbcTemplate.queryForObject(COUNT_CUSTOMERS, Long.class,
            pattern, pattern, pattern, pattern, pattern, pattern, pattern, pattern);
        return total == null ? 0 : total;
    }

    private static String likePattern(String text) {
        String escaped = text.toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static CustomerProfile mapCustomer(ResultSet resultSet, int rowNumber) throws SQLException {
        String gender = resultSet.getString("gender");
        String maritalStatus = resultSet.getString("marital_status");
        BigDecimal annualIncome = resultSet.getBigDecimal("annual_income");
        String addressLine1 = resultSet.getString("address_line1");
        String nomineeName = resultSet.getString("nominee_name");
        return new CustomerProfile(
            resultSet.getString("customer_id"),
            resultSet.getString("customer_number"),
            resultSet.getString("full_name"),
            resultSet.getString("email"),
            resultSet.getString("phone"),
            resultSet.getDate("date_of_birth").toLocalDate(),
            gender == null ? null : Gender.valueOf(gender),
            maritalStatus == null ? null : MaritalStatus.valueOf(maritalStatus),
            resultSet.getString("occupation"),
            annualIncome,
            addressLine1 == null ? null : new Address(
                addressLine1,
                resultSet.getString("address_line2"),
                resultSet.getString("address_city"),
                resultSet.getString("address_state"),
                resultSet.getString("address_postal_code"),
                resultSet.getString("address_country")
            ),
            nomineeName == null ? null : new Nominee(nomineeName, resultSet.getString("nominee_relationship")),
            instant(resultSet, "created_at"),
            instant(resultSet, "updated_at")
        );
    }

    private static Instant instant(ResultSet resultSet, String column) throws SQLException {
        return resultSet.getObject(column, OffsetDateTime.class).toInstant();
    }
}
