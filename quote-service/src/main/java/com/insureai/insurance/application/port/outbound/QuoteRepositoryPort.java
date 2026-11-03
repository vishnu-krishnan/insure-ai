package com.insureai.insurance.application.port.outbound;

import java.util.List;
import java.util.Optional;

import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PolicyQuote;

public interface QuoteRepositoryPort {
    PolicyQuote save(PolicyQuote quote);

    Optional<PolicyQuote> findById(String quoteId);

    List<PolicyQuote> findPage(String query, CoverageType coverageType, long offset, int limit);

    long count(String query, CoverageType coverageType);

    List<PolicyQuote> findPageForCustomer(String customerId, long offset, int limit);

    long countForCustomer(String customerId);

    Optional<PolicyQuote> findByIdForCustomer(String quoteId, String customerId);
}
