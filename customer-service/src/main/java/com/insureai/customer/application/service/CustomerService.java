package com.insureai.customer.application.service;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.insureai.customer.application.port.inbound.CreateCustomerCommand;
import com.insureai.customer.application.port.inbound.CreateCustomerUseCase;
import com.insureai.customer.application.port.inbound.GetCustomerUseCase;
import com.insureai.customer.application.port.outbound.CustomerRepositoryPort;
import com.insureai.customer.domain.exception.CustomerNotFoundException;
import com.insureai.customer.domain.exception.CustomerValidationException;
import com.insureai.customer.domain.model.CustomerProfile;

@Service
public class CustomerService implements CreateCustomerUseCase, GetCustomerUseCase {
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
            command.dateOfBirth()
        );
        customerRepository.save(customer);
        return customer;
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
}
