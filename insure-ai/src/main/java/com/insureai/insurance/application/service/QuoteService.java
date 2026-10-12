package com.insureai.insurance.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.insureai.insurance.application.port.inbound.CreateQuoteCommand;
import com.insureai.insurance.application.port.inbound.CreateQuoteUseCase;
import com.insureai.insurance.application.port.outbound.QuoteRepositoryPort;
import com.insureai.insurance.domain.exception.DomainValidationException;
import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PolicyQuote;

@Service
public class QuoteService implements CreateQuoteUseCase {
    private final QuoteRepositoryPort quoteRepository;

    public QuoteService(QuoteRepositoryPort quoteRepository) {
        this.quoteRepository = quoteRepository;
    }

    @Override
    public PolicyQuote createQuote(CreateQuoteCommand command) {
        String customerName = normalizeCustomerName(command.customerName());
        CoverageType coverageType = CoverageType.fromCode(command.coverageType());

        if (command.age() < 18 || command.age() > 75) {
            throw new DomainValidationException("age must be between 18 and 75");
        }

        if (command.coverageAmount() == null || command.coverageAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("coverageAmount must be greater than zero");
        }

        BigDecimal rateMultiplier = command.age() > 60 ? new BigDecimal("1.15") : BigDecimal.ONE;
        BigDecimal annualPremium = command.coverageAmount()
            .multiply(coverageType.getBaseRate())
            .multiply(rateMultiplier)
            .setScale(2, RoundingMode.HALF_UP);

        PolicyQuote quote = new PolicyQuote(
            UUID.randomUUID().toString(),
            customerName,
            coverageType,
            command.age(),
            command.coverageAmount(),
            annualPremium
        );

        quoteRepository.save(quote);
        return quote;
    }

    private String normalizeCustomerName(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            throw new DomainValidationException("customerName is required");
        }

        String trimmed = customerName.trim();
        if (trimmed.length() < 2 || trimmed.length() > 80) {
            throw new DomainValidationException("customerName must be between 2 and 80 characters");
        }

        if (!trimmed.matches("^[A-Za-z .'-]+$")) {
            throw new DomainValidationException("customerName contains invalid characters");
        }

        return trimmed;
    }
}
