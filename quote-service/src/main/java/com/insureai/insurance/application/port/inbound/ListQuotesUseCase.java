package com.insureai.insurance.application.port.inbound;

public interface ListQuotesUseCase {
    QuotePage listQuotes(int page, int size, String query);
}
