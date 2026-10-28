package com.insureai.customer.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.insureai.customer.application.port.inbound.CreateCustomerCommand;
import com.insureai.customer.application.port.inbound.CustomerPage;
import com.insureai.customer.application.port.outbound.CustomerRepositoryPort;
import com.insureai.customer.domain.exception.CustomerNotFoundException;
import com.insureai.customer.domain.exception.DuplicateCustomerException;
import com.insureai.customer.domain.exception.CustomerValidationException;
import com.insureai.customer.domain.model.Address;
import com.insureai.customer.domain.model.CustomerProfile;
import com.insureai.customer.domain.model.Gender;
import com.insureai.customer.domain.model.MaritalStatus;
import com.insureai.customer.domain.model.Nominee;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {
    @Mock
    private CustomerRepositoryPort customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    void createsCustomerWithNormalizedNameAndEmail() {
        when(customerRepository.save(any())).thenAnswer(invocation -> {
            CustomerProfile unsaved = invocation.getArgument(0);
            return new CustomerProfile(
                unsaved.customerId(), "CUS-000007", unsaved.fullName(), unsaved.email(), unsaved.phone(), unsaved.dateOfBirth(),
                unsaved.gender(), unsaved.maritalStatus(), unsaved.occupation(), unsaved.annualIncome(),
                unsaved.address(), unsaved.nominee(), Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z")
            );
        });

        CustomerProfile customer = customerService.createCustomer(new CreateCustomerCommand(
            " Alice Smith ",
            " ALICE@EXAMPLE.TEST ",
            "+14155552671",
            LocalDate.of(1990, 1, 1),
            null, null, null, null, null, null
        ));

        assertEquals("Alice Smith", customer.fullName());
        assertEquals("alice@example.test", customer.email());
        assertEquals("CUS-000007", customer.customerNumber());
        verify(customerRepository).save(argThat(saved -> saved.customerId().equals(customer.customerId())));
    }

    @Test
    void passesNormalizedDetailsToRepository() {
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerProfile customer = customerService.createCustomer(new CreateCustomerCommand(
            "Alice Smith", "alice@example.test", "+14155552671", LocalDate.of(1990, 1, 1),
            Gender.FEMALE, MaritalStatus.MARRIED, "  Engineer ", new BigDecimal("52000.50"),
            new Address(" 1 Main St ", "  ", " Pune ", "MH", "411001", "in"),
            new Nominee(" Nina Rao ", " Spouse ")
        ));

        assertEquals("Engineer", customer.occupation());
        assertEquals(new Address("1 Main St", null, "Pune", "MH", "411001", "IN"), customer.address());
        assertEquals("IN", customer.address().country());
        assertEquals(new Nominee("Nina Rao", "Spouse"), customer.nominee());
        assertEquals(Gender.FEMALE, customer.gender());
    }

    @Test
    void rejectsFutureDateOfBirth() {
        assertThrows(CustomerValidationException.class, () -> customerService.createCustomer(
            new CreateCustomerCommand("Alice Smith", "alice@example.test", "+14155552671", LocalDate.now().plusDays(1),
                null, null, null, null, null, null)
        ));
    }

    @Test
    void rejectsDuplicateEmailComparedCaseInsensitively() {
        when(customerRepository.existsByEmail("alice@example.test")).thenReturn(true);

        DuplicateCustomerException exception = assertThrows(DuplicateCustomerException.class, () -> customerService.createCustomer(
            new CreateCustomerCommand("Alice Smith", " ALICE@Example.TEST ", "+14155552671", LocalDate.of(1990, 1, 1),
                null, null, null, null, null, null)
        ));

        assertEquals("email", exception.field());
        assertEquals("A customer with this email already exists", exception.getMessage());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void rejectsDuplicatePhoneComparedAfterTrimming() {
        when(customerRepository.existsByPhone("+14155552671")).thenReturn(true);

        DuplicateCustomerException exception = assertThrows(DuplicateCustomerException.class, () -> customerService.createCustomer(
            new CreateCustomerCommand("Alice Smith", "alice@example.test", " +14155552671 ", LocalDate.of(1990, 1, 1),
                null, null, null, null, null, null)
        ));

        assertEquals("phone", exception.field());
        assertEquals("A customer with this phone number already exists", exception.getMessage());
        verify(customerRepository, never()).save(any());
    }

    @Test
    void rejectsUnderageAndOverageCustomers() {
        LocalDate today = LocalDate.now();
        for (LocalDate dateOfBirth : List.of(today.minusYears(18).plusDays(1), today.minusYears(76))) {
            assertThrows(CustomerValidationException.class, () -> customerService.createCustomer(
                new CreateCustomerCommand("Alice Smith", "alice@example.test", "+14155552671", dateOfBirth,
                    null, null, null, null, null, null)
            ), dateOfBirth.toString());
        }
        verifyNoInteractions(customerRepository);
    }

    @Test
    void acceptsCustomersAtAgeBoundaries() {
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDate today = LocalDate.now();

        for (LocalDate dateOfBirth : List.of(today.minusYears(18), today.minusYears(75))) {
            assertEquals(dateOfBirth, customerService.createCustomer(
                new CreateCustomerCommand("Alice Smith", "alice@example.test", "+14155552671", dateOfBirth,
                    null, null, null, null, null, null)
            ).dateOfBirth());
        }
    }

    @Test
    void rejectsFullNamesWithDigitsOrSymbols() {
        for (String name : List.of("Alice 2", "Alice <script>", "1234", "Alice_Smith", "--", "Al!ce")) {
            assertThrows(CustomerValidationException.class, () -> customerService.createCustomer(
                new CreateCustomerCommand(name, "alice@example.test", "+14155552671", LocalDate.of(1990, 1, 1),
                    null, null, null, null, null, null)
            ), name);
        }
        verifyNoInteractions(customerRepository);
    }

    @Test
    void acceptsRealisticFullNames() {
        when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        for (String name : List.of("Alice Smith", "Mary-Jane O'Neil", "Dr. Ana de la Cruz", "Jos\u00e9 N\u00fa\u00f1ez", "Li Wei")) {
            assertEquals(name, customerService.createCustomer(
                new CreateCustomerCommand(name, "alice@example.test", "+14155552671", LocalDate.of(1990, 1, 1),
                    null, null, null, null, null, null)
            ).fullName());
        }
    }

    @Test
    void returnsCustomerByCanonicalUuid() {
        CustomerProfile customer = new CustomerProfile(
            "3b241101-e2bb-4255-8caf-4136c566a962",
            "CUS-000001",
            "Alice Smith",
            "alice@example.test",
            "+14155552671",
            LocalDate.of(1990, 1, 1),
            null, null, null, null, null, null, null, null
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

    @Test
    void searchesWithTrimmedTextAndComputedOffset() {
        CustomerProfile customer = new CustomerProfile(
            "3b241101-e2bb-4255-8caf-4136c566a962",
            "CUS-000001",
            "Alice Smith",
            "alice@example.test",
            "+14155552671",
            LocalDate.of(1990, 1, 1),
            null, null, null, null, null, null, null, null
        );
        when(customerRepository.search("alice", 40, 20)).thenReturn(List.of(customer));
        when(customerRepository.countSearch("alice")).thenReturn(41L);

        CustomerPage page = customerService.searchCustomers("  alice ", 2, 20);

        assertEquals(List.of(customer), page.content());
        assertEquals(2, page.page());
        assertEquals(20, page.size());
        assertEquals(41, page.totalElements());
        assertEquals(3, page.totalPages());
    }

    @Test
    void blankSearchTextMatchesEverything() {
        when(customerRepository.search("", 0, 20)).thenReturn(List.of());
        when(customerRepository.countSearch("")).thenReturn(0L);

        assertEquals(0, customerService.searchCustomers("   ", 0, 20).totalPages());
        assertEquals(0, customerService.searchCustomers(null, 0, 20).content().size());
    }

    @Test
    void rejectsInvalidSearchParameters() {
        assertThrows(CustomerValidationException.class, () -> customerService.searchCustomers("x".repeat(81), 0, 20));
        assertThrows(CustomerValidationException.class, () -> customerService.searchCustomers("a", -1, 20));
        assertThrows(CustomerValidationException.class, () -> customerService.searchCustomers("a", 0, 0));
        assertThrows(CustomerValidationException.class, () -> customerService.searchCustomers("a", 0, 101));
        verifyNoInteractions(customerRepository);
    }
}
