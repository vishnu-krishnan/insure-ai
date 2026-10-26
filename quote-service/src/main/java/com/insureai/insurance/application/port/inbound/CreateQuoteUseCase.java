package com.insureai.insurance.application.port.inbound;

import com.insureai.insurance.domain.model.PolicyQuote;

public interface CreateQuoteUseCase {
    PolicyQuote createQuote(CreateQuoteCommand command);
}
