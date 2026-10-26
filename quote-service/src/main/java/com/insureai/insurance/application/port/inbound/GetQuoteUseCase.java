package com.insureai.insurance.application.port.inbound;

import com.insureai.insurance.domain.model.PolicyQuote;

public interface GetQuoteUseCase {
    PolicyQuote getQuoteById(String quoteId);
}
