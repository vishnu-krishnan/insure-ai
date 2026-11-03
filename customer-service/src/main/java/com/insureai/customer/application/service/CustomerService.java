package com.insureai.customer.application.service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.insureai.customer.application.port.inbound.CreateCustomerCommand;
import com.insureai.customer.application.port.inbound.CreateCustomerUseCase;
import com.insureai.customer.application.port.inbound.CustomerPage;
import com.insureai.customer.application.port.inbound.GetCustomerUseCase;
import com.insureai.customer.application.port.inbound.MyProfileUseCase;
import com.insureai.customer.application.port.inbound.SearchCustomersUseCase;
import com.insureai.customer.application.port.outbound.CustomerRepositoryPort;
import com.insureai.customer.domain.exception.CustomerNotFoundException;
import com.insureai.customer.domain.exception.CustomerValidationException;
import com.insureai.customer.domain.exception.DuplicateCustomerException;
import com.insureai.customer.domain.model.CustomerProfile;
import com.insureai.customer.domain.model.IdDocument;

@Service
public class CustomerService implements CreateCustomerUseCase, GetCustomerUseCase, SearchCustomersUseCase, MyProfileUseCase {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerService.class);
    private static final int MAX_SEARCH_TEXT_LENGTH = 80;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MIN_AGE = 18;
    private static final int MAX_AGE = 75;
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("^[0-9]{8,19}$");
    private static final Pattern FULL_NAME_PATTERN = Pattern.compile("^\\p{L}[\\p{L}\\p{M} .'-]*$");

    private final CustomerRepositoryPort customerRepository;

    public CustomerService(CustomerRepositoryPort customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerProfile createCustomer(CreateCustomerCommand command) {
        validate(command);
        CustomerProfile customer = CustomerProfile.create(
            command.fullName(),
            command.email(),
            command.phone(),
            command.dateOfBirth(),
            command.gender(),
            command.maritalStatus(),
            command.occupation(),
            command.annualIncome(),
            command.address(),
            command.nominee()
        );
        if (customerRepository.existsByEmail(customer.email())) {
            throw new DuplicateCustomerException("email");
        }
        if (customerRepository.existsByPhone(customer.phone())) {
            throw new DuplicateCustomerException("phone");
        }
        CustomerProfile saved = customerRepository.save(customer);
        logCreated(saved);
        return saved;
    }

    private static void validate(CreateCustomerCommand command) {
        if (command.fullName() == null || command.fullName().isBlank()
            || command.fullName().trim().length() < 2 || command.fullName().trim().length() > 80) {
            throw new CustomerValidationException("fullName must be between 2 and 80 characters");
        }
        if (!FULL_NAME_PATTERN.matcher(command.fullName().trim()).matches()) {
            throw new CustomerValidationException("fullName may contain only letters, spaces, apostrophes, hyphens and periods");
        }
        LocalDate today = LocalDate.now();
        if (command.dateOfBirth() == null || !command.dateOfBirth().isBefore(today)) {
            throw new CustomerValidationException("dateOfBirth must be in the past");
        }
        int age = Period.between(command.dateOfBirth(), today).getYears();
        if (age < MIN_AGE || age > MAX_AGE) {
            throw new CustomerValidationException("customer age must be between " + MIN_AGE + " and " + MAX_AGE + " years");
        }
        IdDocument idDocument = command.idDocument();
        if (idDocument != null && !idDocument.type().accepts(idDocument.number())) {
            throw new CustomerValidationException(idDocument.type().message());
        }

    }

    private static void logCreated(CustomerProfile saved) {
        LOGGER.atInfo()
            .setMessage("Customer created: customerId={} customerNumber={}")
            .addArgument(saved.customerId())
            .addArgument(saved.customerNumber())
            .addKeyValue("event.action", "customer.created")
            .addKeyValue("customerId", saved.customerId())
            .addKeyValue("customerNumber", saved.customerNumber())
            .log();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfile getCustomerById(String customerId) {
        if (customerId == null || !CUSTOMER_ID_PATTERN.matcher(customerId).matches()) {
            throw new CustomerValidationException("customerId must be a numeric ID of 8 to 19 digits");
        }

        return customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerPage searchCustomers(String text, int page, int size) {
        if (text != null && text.length() > MAX_SEARCH_TEXT_LENGTH) {
            throw new CustomerValidationException("q must be at most " + MAX_SEARCH_TEXT_LENGTH + " characters");
        }
        if (page < 0) {
            throw new CustomerValidationException("page must be zero or greater");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new CustomerValidationException("size must be between 1 and " + MAX_PAGE_SIZE);
        }

        String criteria = text == null ? "" : text.trim();
        List<CustomerProfile> content = customerRepository.search(criteria, (long) page * size, size);
        return new CustomerPage(content, page, size, customerRepository.countSearch(criteria));
    }

    @Override
    @Transactional
    public Optional<CustomerProfile> findMine(String subject, String email, boolean emailVerified) {
        Optional<CustomerProfile> owned = customerRepository.findBySubject(subject);
        if (owned.isPresent() || !emailVerified || email == null || email.isBlank()) {
            return owned;
        }
        Optional<CustomerProfile> byEmail = customerRepository.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (byEmail.isPresent() && customerRepository.linkSubject(byEmail.get().customerId(), subject)) {
            LOGGER.atInfo()
                .setMessage("Customer profile claimed by sign-up: customerId={}")
                .addArgument(byEmail.get().customerId())
                .addKeyValue("event.action", "customer.claimed")
                .addKeyValue("customerId", byEmail.get().customerId())
                .log();
            return customerRepository.findBySubject(subject);
        }
        return Optional.empty();
    }

    @Override
    @Transactional
    public CustomerProfile registerMine(String subject, String email, CreateCustomerCommand command) {
        if (customerRepository.findBySubject(subject).isPresent()) {
            throw new DuplicateCustomerException("profile");
        }
        CreateCustomerCommand withEmail = new CreateCustomerCommand(
            command.fullName(), email, command.phone(), command.dateOfBirth(), command.gender(),
            command.maritalStatus(), command.occupation(), command.annualIncome(), command.address(), command.nominee(),
            command.idDocument()
        );
        validate(withEmail);
        CustomerProfile customer = CustomerProfile.create(
            withEmail.fullName(), withEmail.email(), withEmail.phone(), withEmail.dateOfBirth(), withEmail.gender(),
            withEmail.maritalStatus(), withEmail.occupation(), withEmail.annualIncome(), withEmail.address(),
            withEmail.nominee(), withEmail.idDocument()
        );
        if (customerRepository.existsByEmail(customer.email())) {
            throw new DuplicateCustomerException("email");
        }
        if (customerRepository.existsByPhone(customer.phone())) {
            throw new DuplicateCustomerException("phone");
        }
        CustomerProfile saved = customerRepository.save(customer, subject);
        logCreated(saved);
        return saved;
    }

    @Override
    @Transactional
    public CustomerProfile updateMine(String subject, CreateCustomerCommand command) {
        CustomerProfile current = customerRepository.findBySubject(subject)
            .orElseThrow(() -> new CustomerNotFoundException("me"));
        validate(command);
        CustomerProfile edited = CustomerProfile.create(
            command.fullName(), current.email(), command.phone(), command.dateOfBirth(), command.gender(),
            command.maritalStatus(), command.occupation(), command.annualIncome(), command.address(), command.nominee(),
            command.idDocument()
        );
        if (customerRepository.existsByPhoneExcluding(edited.phone(), current.customerId())) {
            throw new DuplicateCustomerException("phone");
        }
        CustomerProfile saved = customerRepository.update(new CustomerProfile(
            current.customerId(), current.customerNumber(), edited.fullName(), edited.email(), edited.phone(),
            edited.dateOfBirth(), edited.gender(), edited.maritalStatus(), edited.occupation(), edited.annualIncome(),
            edited.address(), edited.nominee(), current.createdAt(), current.updatedAt(), edited.idDocument()
        ));
        LOGGER.atInfo()
            .setMessage("Customer updated own profile: customerId={}")
            .addArgument(saved.customerId())
            .addKeyValue("event.action", "customer.updated")
            .addKeyValue("customerId", saved.customerId())
            .log();
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerProfile> findBySubject(String subject) {
        return customerRepository.findBySubject(subject);
    }
}
