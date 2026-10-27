package com.insureai.insurance.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import com.insureai.insurance.domain.model.PaymentFrequency;
import com.insureai.insurance.domain.model.PolicyQuote;

@Service
public class QuoteService
    implements CreateQuoteUseCase, CreateCustomerQuoteUseCase, GetQuoteUseCase, ListQuotesUseCase {
    public static final int MAX_PAGE_SIZE = 100;
    public static final int MAX_SEARCH_LENGTH = 80;
    public static final int MIN_POLICY_TERM_YEARS = 1;
    public static final int MAX_POLICY_TERM_YEARS = 40;
    public static final int MAX_START_DATE_OFFSET_DAYS = 365;
    private static final int TAX_RATE_SCALE = 4;

    private final QuoteRepositoryPort quoteRepository;
    private final CustomerProfileLookupPort customerProfileLookup;
    private final Clock clock;
    private final BigDecimal taxRate;

    @Autowired
    public QuoteService(
        QuoteRepositoryPort quoteRepository,
        CustomerProfileLookupPort customerProfileLookup,
        Clock clock,
        @Value("${quote.tax-rate:0.18}") BigDecimal taxRate
    ) {
        Objects.requireNonNull(taxRate, "quote.tax-rate is required");
        if (taxRate.signum() < 0 || taxRate.compareTo(BigDecimal.ONE) > 0
            || taxRate.stripTrailingZeros().scale() > TAX_RATE_SCALE) {
            throw new IllegalArgumentException(
                "quote.tax-rate must be between 0 and 1 with at most " + TAX_RATE_SCALE + " decimal places");
        }
        this.quoteRepository = quoteRepository;
        this.customerProfileLookup = customerProfileLookup;
        this.clock = clock;
        this.taxRate = taxRate.setScale(TAX_RATE_SCALE, RoundingMode.UNNECESSARY);
    }

    @Override
    @Transactional
    public PolicyQuote createQuote(CreateQuoteCommand command) {
        String customerName = normalizeCustomerName(command.customerName());
        CoverageType coverageType = CoverageType.fromCode(command.coverageType());

        validateCoverageAmount(command.coverageAmount());
        PolicyDetails details = resolveDetails(
            command.policyTermYears(), command.paymentFrequency(), command.deductible(), command.startDate(),
            command.coverageAmount(), command.createdBy());

        return createQuote(null, null, customerName, coverageType, command.age(), command.coverageAmount(), details);
    }

    @Override
    @Transactional
    public PolicyQuote createQuoteForCustomer(CreateCustomerQuoteCommand command) {
        CoverageType coverageType = CoverageType.fromCode(command.coverageType());
        validateCoverageAmount(command.coverageAmount());
        PolicyDetails details = resolveDetails(
            command.policyTermYears(), command.paymentFrequency(), command.deductible(), command.startDate(),
            command.coverageAmount(), command.createdBy());

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
        return createQuote(customerId, customer.customerNumber(), customerName, coverageType, age,
            command.coverageAmount(), details);
    }

    @Override
    @Transactional(readOnly = true)
    public PolicyQuote getQuoteById(String quoteId) {
        return quoteRepository.findById(quoteId)
            .orElseThrow(() -> new QuoteNotFoundException(quoteId));
    }

    @Override
    @Transactional(readOnly = true)
    public QuotePage listQuotes(int page, int size, String query) {
        if (page < 0) {
            throw new DomainValidationException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new DomainValidationException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        String searchTerm = query == null ? "" : query.trim();
        if (searchTerm.length() > MAX_SEARCH_LENGTH) {
            throw new DomainValidationException("q must be at most " + MAX_SEARCH_LENGTH + " characters");
        }

        List<PolicyQuote> content = quoteRepository.findPage(searchTerm, (long) page * size, size);
        return new QuotePage(content, page, size, quoteRepository.count(searchTerm));
    }

    private PolicyQuote createQuote(
        String customerId,
        String customerNumber,
        String customerName,
        CoverageType coverageType,
        int age,
        BigDecimal coverageAmount,
        PolicyDetails details
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
        BigDecimal taxAmount = annualPremium.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        PolicyQuote quote = new PolicyQuote(
            UUID.randomUUID().toString(),
            customerName,
            coverageType,
            age,
            coverageAmount,
            annualPremium,
            customerId,
            null,
            customerNumber,
            details.policyTermYears(),
            details.paymentFrequency(),
            details.deductible(),
            details.startDate(),
            taxRate,
            taxAmount,
            Instant.now(clock).truncatedTo(ChronoUnit.MICROS),
            details.createdBy()
        );
        return quoteRepository.save(quote);
    }

    private PolicyDetails resolveDetails(
        Integer policyTermYears,
        String paymentFrequency,
        BigDecimal deductible,
        LocalDate startDate,
        BigDecimal coverageAmount,
        String createdBy
    ) {
        int term = policyTermYears == null ? MIN_POLICY_TERM_YEARS : policyTermYears;
        if (term < MIN_POLICY_TERM_YEARS || term > MAX_POLICY_TERM_YEARS) {
            throw new DomainValidationException(
                "policyTermYears must be between " + MIN_POLICY_TERM_YEARS + " and " + MAX_POLICY_TERM_YEARS,
                "policyTermYears");
        }
        PaymentFrequency frequency = paymentFrequency == null
            ? PaymentFrequency.YEARLY
            : PaymentFrequency.fromCode(paymentFrequency);

        BigDecimal resolvedDeductible = deductible == null ? BigDecimal.ZERO : deductible;
        if (resolvedDeductible.signum() < 0) {
            throw new DomainValidationException("deductible must be zero or greater", "deductible");
        }
        resolvedDeductible = resolvedDeductible.setScale(2, RoundingMode.HALF_UP);
        if (resolvedDeductible.compareTo(coverageAmount) >= 0) {
            throw new DomainValidationException("deductible must be less than coverageAmount", "deductible");
        }

        LocalDate today = LocalDate.now(clock.withZone(ZoneOffset.UTC));
        LocalDate resolvedStart = startDate == null ? today : startDate;
        if (resolvedStart.isBefore(today) || resolvedStart.isAfter(today.plusDays(MAX_START_DATE_OFFSET_DAYS))) {
            throw new DomainValidationException(
                "startDate must be between today and " + MAX_START_DATE_OFFSET_DAYS + " days from today",
                "startDate");
        }
        return new PolicyDetails(term, frequency, resolvedDeductible, resolvedStart, createdBy);
    }

    private record PolicyDetails(
        int policyTermYears,
        PaymentFrequency paymentFrequency,
        BigDecimal deductible,
        LocalDate startDate,
        String createdBy
    ) {
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
