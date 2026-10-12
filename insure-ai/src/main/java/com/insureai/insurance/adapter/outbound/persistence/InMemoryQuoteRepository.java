package com.insureai.insurance.adapter.outbound.persistence;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.insureai.insurance.application.port.outbound.QuoteRepositoryPort;
import com.insureai.insurance.domain.model.PolicyQuote;

@Repository
public class InMemoryQuoteRepository implements QuoteRepositoryPort {
    private final Map<String, PolicyQuote> quotes = new ConcurrentHashMap<>();

    @Override
    public void save(PolicyQuote quote) {
        quotes.put(quote.quoteId(), quote);
    }

    @Override
    public List<PolicyQuote> findAll() {
        return List.copyOf(quotes.values());
    }
}
