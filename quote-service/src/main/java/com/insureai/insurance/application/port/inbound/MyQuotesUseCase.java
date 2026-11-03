package com.insureai.insurance.application.port.inbound;

import com.insureai.insurance.domain.model.PolicyQuote;

/** Quotes for the signed-in customer, found through their Keycloak subject; nobody else's quotes are reachable. */
public interface MyQuotesUseCase {
    QuotePage listMine(String subject, int page, int size);

    PolicyQuote getMine(String subject, String quoteId);

    PolicyQuote createMine(String subject, CreateCustomerQuoteCommand command);
}
