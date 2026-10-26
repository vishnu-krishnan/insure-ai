package com.insureai.customer.adapter.outbound.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.insureai.customer.application.port.outbound.CustomerRepositoryPort;
import com.insureai.customer.domain.model.CustomerProfile;

@Repository
public class PostgresCustomerRepository implements CustomerRepositoryPort {
    private static final String INSERT_CUSTOMER = """
        INSERT INTO customer_profile (customer_id, full_name, email, phone, date_of_birth)
        VALUES (?, ?, ?, ?, ?)
        """;
    private static final String SELECT_CUSTOMER = """
        SELECT customer_id, full_name, email, phone, date_of_birth
        FROM customer_profile
        WHERE customer_id = ?
        """;
    private static final RowMapper<CustomerProfile> CUSTOMER_ROW_MAPPER = PostgresCustomerRepository::mapCustomer;

    private final JdbcTemplate jdbcTemplate;

    public PostgresCustomerRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(CustomerProfile customer) {
        jdbcTemplate.update(
            INSERT_CUSTOMER,
            customer.customerId(),
            customer.fullName(),
            customer.email(),
            customer.phone(),
            customer.dateOfBirth()
        );
    }

    @Override
    public Optional<CustomerProfile> findById(String customerId) {
        List<CustomerProfile> customers = jdbcTemplate.query(SELECT_CUSTOMER, CUSTOMER_ROW_MAPPER, customerId);
        return customers.stream().findFirst();
    }

    private static CustomerProfile mapCustomer(ResultSet resultSet, int rowNumber) throws SQLException {
        return new CustomerProfile(
            resultSet.getString("customer_id"),
            resultSet.getString("full_name"),
            resultSet.getString("email"),
            resultSet.getString("phone"),
            resultSet.getDate("date_of_birth").toLocalDate()
        );
    }
}
