package com.insureai.customer.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.insureai.customer.application.port.inbound.CreateCustomerCommand;
import com.insureai.customer.application.port.outbound.CustomerRepositoryPort;
import com.insureai.customer.domain.exception.CustomerNotFoundException;
import com.insureai.customer.domain.exception.CustomerValidationException;
import com.insureai.customer.domain.model.CustomerProfile;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock
    private CustomerRepositoryPort customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createsCustomerWithNormalizedNameAndEmail() {
        CustomerProfile customer = customerService.createCustomer(new CreateCustomerCommand(
            " Alice Smith ",
            " ALICE@EXAMPLE.TEST ",
            "+14155552671",
            LocalDate.of(1990, 1, 1)
        ));

        assertEquals("Alice Smith", customer.fullName());
        assertEquals("alice@example.test", customer.email());
        verify(customerRepository).save(customer);
    }

    @Test
    void rejectsFutureDateOfBirth() {
        assertThrows(CustomerValidationException.class, () -> customerService.createCustomer(
            new CreateCustomerCommand("Alice Smith", "alice@example.test", "+14155552671", LocalDate.now().plusDays(1))
        ));
    }

    @Test
    void returnsCustomerByCanonicalUuid() {
        CustomerProfile customer = new CustomerProfile(
            "3b241101-e2bb-4255-8caf-4136c566a962",
            "Alice Smith",
            "alice@example.test",
            "+14155552671",
            LocalDate.of(1990, 1, 1)
        );
        when(customerRepository.findById(customer.customerId())).thenReturn(Optional.of(customer));

        assertEquals(customer, customerService.getCustomerById(customer.customerId().toUpperCase()));
    }

    @Test
    void reportsUnknownCustomer() {
        when(customerRepository.findById("3b241101-e2bb-4255-8caf-4136c566a962")).thenReturn(Optional.empty());

        assertThrows(
            CustomerNotFoundException.class,
            () -> customerService.getCustomerById("3b241101-e2bb-4255-8caf-4136c566a962")
        );
    }
}
