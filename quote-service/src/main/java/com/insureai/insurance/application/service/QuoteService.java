package com.insureai.insurance.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.insureai.insurance.application.port.inbound.CreateCustomerQuoteCommand;
import com.insureai.insurance.application.port.inbound.CreateCustomerQuoteUseCase;
import com.insureai.insurance.application.port.inbound.CreateQuoteCommand;
import com.insureai.insurance.application.port.inbound.CreateQuoteUseCase;
import com.insureai.insurance.application.port.inbound.GetQuoteUseCase;
import com.insureai.insurance.application.port.inbound.ListQuotesUseCase;
import com.insureai.insurance.application.port.inbound.QuotePage;
import com.insureai.insurance.application.port.outbound.CustomerProfileLookupPort;
import com.insureai.insurance.application.port.outbound.CustomerProfileSnapshot;
import com.insureai.insurance.application.port.outbound.QuoteRepositoryPort;
import com.insureai.insurance.domain.exception.CustomerReferenceNotFoundException;
import com.insureai.insurance.domain.exception.DomainValidationException;
import com.insureai.insurance.domain.exception.QuoteNotFoundException;
import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PolicyQuote;

@Service
public class QuoteService
    implements CreateQuoteUseCase, CreateCustomerQuoteUseCase, GetQuoteUseCase, ListQuotesUseCase {
    public static final int MAX_PAGE_SIZE = 100;

    private final QuoteRepositoryPort quoteRepository;
    private final CustomerProfileLookupPort customerProfileLookup;
    private final Clock clock;

    @Autowired
    public QuoteService(
        QuoteRepositoryPort quoteRepository,
        CustomerProfileLookupPort customerProfileLookup,
        Clock clock
    ) {
        this.quoteRepository = quoteRepository;
        this.customerProfileLookup = customerProfileLookup;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PolicyQuote createQuote(CreateQuoteCommand command) {
        String customerName = normalizeCustomerName(command.customerName());
        CoverageType coverageType = CoverageType.fromCode(command.coverageType());

        return createQuote(null, customerName, coverageType, command.age(), command.coverageAmount());
    }

    @Override
    @Transactional
    public PolicyQuote createQuoteForCustomer(CreateCustomerQuoteCommand command) {
        CoverageType coverageType = CoverageType.fromCode(command.coverageType());
        validateCoverageAmount(command.coverageAmount());

        String customerId;
        try {
            customerId = UUID.fromString(command.customerId()).toString();
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new DomainValidationException("customerId must be a valid UUID");
        }

        CustomerProfileSnapshot customer = customerProfileLookup.findCustomer(customerId);
        if (!customerId.equals(customer.customerId())) {
            throw new CustomerReferenceNotFoundException(customerId);
        }
        String customerName = customer.fullName() == null ? null : customer.fullName().trim();
        if (customerName == null || customerName.length() < 2 || customerName.length() > 80) {
            throw new DomainValidationException("Customer profile fullName must be between 2 and 80 characters");
        }
        if (customer.dateOfBirth() == null || !customer.dateOfBirth().isBefore(LocalDate.now(clock))) {
            throw new DomainValidationException("Customer profile dateOfBirth must be in the past");
        }
        int age = Period.between(customer.dateOfBirth(), LocalDate.now(clock)).getYears();
        return createQuote(customerId, customerName, coverageType, age, command.coverageAmount());
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyQuote getQuoteById(String quoteId) {
        return quoteRepository.findById(quoteId)
            .orElseThrow(() -> new QuoteNotFoundException(quoteId));
    }

    @Override
    @Transactional(readOnly = true)
    public QuotePage listQuotes(int page, int size) {
        if (page < 0) {
            throw new DomainValidationException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainValidationException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        List<PolicyQuote> content = quoteRepository.findPage((long) page * size, size);
        return new QuotePage(content, page, size, quoteRepository.count());
    }

    private PolicyQuote createQuote(
        String customerId,
        String customerName,
        CoverageType coverageType,
        int age,
        BigDecimal coverageAmount
    ) {
        if (age < 18 || age > 75) {
            throw new DomainValidationException("age must be between 18 and 75");
        }
        validateCoverageAmount(coverageAmount);

        BigDecimal rateMultiplier = age > 60 ? new BigDecimal("1.15") : BigDecimal.ONE;
        BigDecimal annualPremium = coverageAmount
            .multiply(coverageType.getBaseRate())
            .multiply(rateMultiplier)
            .setScale(2, RoundingMode.HALF_UP);

        PolicyQuote quote = new PolicyQuote(
            UUID.randomUUID().toString(),
            customerName,
            coverageType,
            age,
            coverageAmount,
            annualPremium,
            customerId
        );
        quoteRepository.save(quote);
        return quote;
    }

    private static void validateCoverageAmount(BigDecimal coverageAmount) {
        if (coverageAmount == null || coverageAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainValidationException("coverageAmount must be greater than zero");
        }
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
