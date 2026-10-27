package com.insureai.insurance.adapter.outbound.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.insureai.insurance.application.port.outbound.QuoteRepositoryPort;
import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PaymentFrequency;
import com.insureai.insurance.domain.model.PolicyQuote;

@Repository
public class PostgresQuoteRepository implements QuoteRepositoryPort {
    private static final String INSERT_QUOTE = """
        INSERT INTO policy_quote (
            quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium, customer_id,
            customer_number, policy_term_years, payment_frequency, deductible, start_date, tax_rate, tax_amount,
            created_at, created_by
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING quote_number
        """;
    private static final String QUOTE_COLUMNS = """
        quote_id, quote_number, customer_name, coverage_type, age, coverage_amount, annual_premium,
        customer_id, customer_number, policy_term_years, payment_frequency, deductible, start_date, tax_rate,
        tax_amount, created_at, created_by
        """;
    private static final String SEARCH_FILTER = """
        WHERE quote_number ILIKE ? ESCAPE '\\'
            OR quote_id ILIKE ? ESCAPE '\\'
            OR customer_name ILIKE ? ESCAPE '\\'
            OR customer_number ILIKE ? ESCAPE '\\'
            OR customer_id ILIKE ? ESCAPE '\\'
            OR coverage_type ILIKE ? ESCAPE '\\'
            OR payment_frequency ILIKE ? ESCAPE '\\'
            OR created_by ILIKE ? ESCAPE '\\'
        """;
    private static final int SEARCH_FILTER_PARAMETERS = 8;
    private static final String SELECT_QUOTE_PAGE = "SELECT " + QUOTE_COLUMNS + "FROM policy_quote "
        + SEARCH_FILTER + "ORDER BY LENGTH(quote_number) DESC, quote_number DESC LIMIT ? OFFSET ?";
    private static final String COUNT_QUOTES = "SELECT COUNT(*) FROM policy_quote " + SEARCH_FILTER;
    private static final String SELECT_QUOTE_BY_ID = "SELECT " + QUOTE_COLUMNS
        + "FROM policy_quote WHERE quote_id = ?";
    private static final RowMapper<PolicyQuote> QUOTE_ROW_MAPPER = PostgresQuoteRepository::mapQuote;

    private final JdbcTemplate jdbcTemplate;

    public PostgresQuoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PolicyQuote save(PolicyQuote quote) {
        String quoteNumber = jdbcTemplate.queryForObject(
            INSERT_QUOTE,
            String.class,
            quote.quoteId(),
            quote.customerName(),
            quote.coverageType().getCode(),
            quote.age(),
            quote.coverageAmount(),
            quote.annualPremium(),
            quote.customerId(),
            quote.customerNumber(),
            quote.policyTermYears(),
            quote.paymentFrequency().name(),
            quote.deductible(),
            quote.startDate(),
            quote.taxRate(),
            quote.taxAmount(),
            OffsetDateTime.ofInstant(quote.createdAt(), ZoneOffset.UTC),
            quote.createdBy()
        );
        return quote.withQuoteNumber(quoteNumber);
    }

    @Override
    public Optional<PolicyQuote> findById(String quoteId) {
        return jdbcTemplate.query(SELECT_QUOTE_BY_ID, QUOTE_ROW_MAPPER, quoteId)
            .stream()
            .findFirst();
    }

    @Override
    public List<PolicyQuote> findPage(String query, long offset, int limit) {
        Object[] parameters = Arrays.copyOf(searchParameters(query), SEARCH_FILTER_PARAMETERS + 2);
        parameters[SEARCH_FILTER_PARAMETERS] = limit;
        parameters[SEARCH_FILTER_PARAMETERS + 1] = offset;
        return List.copyOf(jdbcTemplate.query(SELECT_QUOTE_PAGE, QUOTE_ROW_MAPPER, parameters));
    }

    @Override
    public long count(String query) {
        Long total = jdbcTemplate.queryForObject(COUNT_QUOTES, Long.class, searchParameters(query));
        return total == null ? 0 : total;
    }

    private static Object[] searchParameters(String query) {
        String escaped = query == null ? "" : query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        Object[] parameters = new Object[SEARCH_FILTER_PARAMETERS];
        Arrays.fill(parameters, "%" + escaped + "%");
        return parameters;
    }

    private static PolicyQuote mapQuote(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PolicyQuote(
            resultSet.getString("quote_id"),
            resultSet.getString("customer_name"),
            CoverageType.fromCode(resultSet.getString("coverage_type")),
            resultSet.getInt("age"),
            resultSet.getBigDecimal("coverage_amount"),
            resultSet.getBigDecimal("annual_premium"),
            resultSet.getString("customer_id"),
            resultSet.getString("quote_number"),
            resultSet.getString("customer_number"),
            resultSet.getInt("policy_term_years"),
            PaymentFrequency.valueOf(resultSet.getString("payment_frequency")),
            resultSet.getBigDecimal("deductible"),
            resultSet.getObject("start_date", LocalDate.class),
            resultSet.getBigDecimal("tax_rate"),
            resultSet.getBigDecimal("tax_amount"),
            resultSet.getObject("created_at", OffsetDateTime.class).toInstant(),
            resultSet.getString("created_by")
        );
    }
}
