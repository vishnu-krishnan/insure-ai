package com.insureai.customer.application.service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.insureai.customer.application.port.inbound.CreateCustomerCommand;
import com.insureai.customer.application.port.inbound.CreateCustomerUseCase;
import com.insureai.customer.application.port.inbound.CustomerPage;
import com.insureai.customer.application.port.inbound.GetCustomerUseCase;
import com.insureai.customer.application.port.inbound.SearchCustomersUseCase;
import com.insureai.customer.application.port.outbound.CustomerRepositoryPort;
import com.insureai.customer.domain.exception.CustomerNotFoundException;
import com.insureai.customer.domain.exception.CustomerValidationException;
import com.insureai.customer.domain.model.CustomerProfile;

@Service
public class CustomerService implements CreateCustomerUseCase, GetCustomerUseCase, SearchCustomersUseCase {
    private static final int MAX_SEARCH_TEXT_LENGTH = 80;
    private static final int MAX_PAGE_SIZE = 100;

    private final CustomerRepositoryPort customerRepository;

    public CustomerService(CustomerRepositoryPort customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional
    public CustomerProfile createCustomer(CreateCustomerCommand command) {
        if (command.fullName() == null || command.fullName().isBlank()
            || command.fullName().trim().length() < 2 || command.fullName().trim().length() > 80) {
            throw new CustomerValidationException("fullName must be between 2 and 80 characters");
        }
        if (command.dateOfBirth() == null || !command.dateOfBirth().isBefore(LocalDate.now())) {
            throw new CustomerValidationException("dateOfBirth must be in the past");
        }

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
        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerProfile getCustomerById(String customerId) {
        String canonicalId;
        try {
            canonicalId = UUID.fromString(customerId).toString();
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new CustomerValidationException("customerId must be a valid UUID");
        }

        return customerRepository.findById(canonicalId)
            .orElseThrow(() -> new CustomerNotFoundException(canonicalId));
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
}
