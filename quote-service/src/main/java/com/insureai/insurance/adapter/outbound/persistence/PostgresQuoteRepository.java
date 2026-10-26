package com.insureai.insurance.adapter.outbound.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.insureai.insurance.application.port.outbound.QuoteRepositoryPort;
import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PolicyQuote;

@Repository
public class PostgresQuoteRepository implements QuoteRepositoryPort {
    private static final String INSERT_QUOTE = """
        INSERT INTO policy_quote (
            quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium, customer_id
        ) VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
    private static final String SELECT_QUOTE_PAGE = """
        SELECT quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium, customer_id
        FROM policy_quote
        ORDER BY quote_id
        LIMIT ? OFFSET ?
        """;
    private static final String COUNT_QUOTES = "SELECT COUNT(*) FROM policy_quote";
    private static final String SELECT_QUOTE_BY_ID = """
        SELECT quote_id, customer_name, coverage_type, age, coverage_amount, annual_premium, customer_id
        FROM policy_quote
        WHERE quote_id = ?
        """;
    private static final RowMapper<PolicyQuote> QUOTE_ROW_MAPPER = PostgresQuoteRepository::mapQuote;

    private final JdbcTemplate jdbcTemplate;

    public PostgresQuoteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(PolicyQuote quote) {
        jdbcTemplate.update(
            INSERT_QUOTE,
            quote.quoteId(),
            quote.customerName(),
            quote.coverageType().getCode(),
            quote.age(),
            quote.coverageAmount(),
            quote.annualPremium(),
            quote.customerId()
        );
    }

    @Override
    public Optional<PolicyQuote> findById(String quoteId) {
        return jdbcTemplate.query(SELECT_QUOTE_BY_ID, QUOTE_ROW_MAPPER, quoteId)
            .stream()
            .findFirst();
    }

    @Override
    public List<PolicyQuote> findPage(long offset, int limit) {
        return List.copyOf(jdbcTemplate.query(SELECT_QUOTE_PAGE, QUOTE_ROW_MAPPER, limit, offset));
    }

    @Override
    public long count() {
        Long total = jdbcTemplate.queryForObject(COUNT_QUOTES, Long.class);
        return total == null ? 0 : total;
    }

    private static PolicyQuote mapQuote(ResultSet resultSet, int rowNumber) throws SQLException {
        return new PolicyQuote(
            resultSet.getString("quote_id"),
            resultSet.getString("customer_name"),
            CoverageType.fromCode(resultSet.getString("coverage_type")),
            resultSet.getInt("age"),
            resultSet.getBigDecimal("coverage_amount"),
            resultSet.getBigDecimal("annual_premium"),
            resultSet.getString("customer_id")
        );
    }
}
