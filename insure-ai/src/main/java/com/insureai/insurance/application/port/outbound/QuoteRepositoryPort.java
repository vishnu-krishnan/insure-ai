package com.insureai.insurance.application.port.outbound;

import java.util.List;

import com.insureai.insurance.domain.model.PolicyQuote;

public interface QuoteRepositoryPort {
    void save(PolicyQuote quote);

    List<PolicyQuote> findAll();
}
