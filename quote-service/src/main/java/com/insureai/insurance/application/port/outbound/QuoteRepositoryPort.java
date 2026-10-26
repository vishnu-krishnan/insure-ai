package com.insureai.insurance.application.port.outbound;

import java.util.List;
import java.util.Optional;

import com.insureai.insurance.domain.model.PolicyQuote;

public interface QuoteRepositoryPort {
    void save(PolicyQuote quote);

    Optional<PolicyQuote> findById(String quoteId);

    List<PolicyQuote> findPage(long offset, int limit);

    long count();
}
