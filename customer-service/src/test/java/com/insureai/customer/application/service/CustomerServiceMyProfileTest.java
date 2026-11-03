package com.insureai.customer.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import com.insureai.customer.domain.exception.DuplicateCustomerException;
import com.insureai.customer.domain.exception.CustomerValidationException;
import com.insureai.customer.domain.model.Address;
import com.insureai.customer.domain.model.CustomerProfile;
import com.insureai.customer.domain.model.Gender;
import com.insureai.customer.domain.model.IdDocument;
import com.insureai.customer.domain.model.IdType;
import com.insureai.customer.domain.model.MaritalStatus;
import com.insureai.customer.domain.model.Nominee;

@ExtendWith(MockitoExtension.class)
class CustomerServiceMyProfileTest {
    private static final CustomerProfile EXISTING = new CustomerProfile(
        "10000001", "CUS-000001", "Alice Smith", "alice@example.test", "+14155552671",
        LocalDate.of(1990, 1, 1), null, null, null, null, null, null, null, null
    );

    @Mock
    private CustomerRepositoryPort customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private static CreateCustomerCommand command(String email, String phone) {
        return command(email, phone, new IdDocument(IdType.PAN, "abcde 1234f"));
    }

    private static CreateCustomerCommand command(String email, String phone, IdDocument idDocument) {
        return new CreateCustomerCommand(
            "Alice Smith", email, phone, LocalDate.of(1990, 1, 1), Gender.FEMALE, MaritalStatus.SINGLE,
            "Engineer", new java.math.BigDecimal("1200000"),
            new Address("12 Lake Road", null, "Pune", "Maharashtra", "411001", "IN"),
            new Nominee("Ravi Smith", "Brother"), idDocument);
    }

    @Test
    void acceptsAPartialProfileSoItCanBeCompletedLater() {
        when(customerRepository.findBySubject("sub-1")).thenReturn(Optional.empty());
        when(customerRepository.save(any(), any())).thenReturn(EXISTING);

        CreateCustomerCommand partial = new CreateCustomerCommand(
            "Alice Smith", null, "+14155552671", LocalDate.of(1990, 1, 1), null, null, null, null, null, null, null);
        customerService.registerMine("sub-1", "alice@example.test", partial);

        verify(customerRepository).save(any(), any());
    }

    @Test
    void rejectsAnIdNumberThatDoesNotMatchItsType() {
        when(customerRepository.findBySubject("sub-1")).thenReturn(Optional.empty());

        assertThrows(CustomerValidationException.class,
            () -> customerService.registerMine("sub-1", "alice@example.test",
                command(null, "+14155552671", new IdDocument(IdType.PAN, "12345"))));
    }

    @Test
    void storesTheIdNumberNormalised() {
        when(customerRepository.findBySubject("sub-1")).thenReturn(Optional.empty());
        when(customerRepository.save(any(), any())).thenReturn(EXISTING);

        customerService.registerMine("sub-1", "alice@example.test", command(null, "+14155552671"));

        verify(customerRepository).save(
            org.mockito.ArgumentMatchers.argThat(profile -> profile.idDocument().number().equals("ABCDE1234F")),
            org.mockito.ArgumentMatchers.eq("sub-1"));
    }

    @Test
    void findsTheProfileOwnedByTheSubject() {
        when(customerRepository.findBySubject("sub-1")).thenReturn(Optional.of(EXISTING));

        assertEquals(Optional.of(EXISTING), customerService.findMine("sub-1", "alice@example.test", true));
        verify(customerRepository, never()).findByEmail(any());
    }

    @Test
    void claimsAnAgentCreatedProfileWhenTheVerifiedEmailMatches() {
        when(customerRepository.findBySubject("sub-1")).thenReturn(Optional.empty(), Optional.of(EXISTING));
        when(customerRepository.findByEmail("alice@example.test")).thenReturn(Optional.of(EXISTING));
        when(customerRepository.linkSubject("10000001", "sub-1")).thenReturn(true);

        assertTrue(customerService.findMine("sub-1", " Alice@Example.test ", true).isPresent());
        verify(customerRepository).linkSubject("10000001", "sub-1");
    }

    @Test
    void doesNotClaimAProfileWhenTheEmailIsNotVerified() {
        when(customerRepository.findBySubject("sub-1")).thenReturn(Optional.empty());

        assertTrue(customerService.findMine("sub-1", "alice@example.test", false).isEmpty());
        verify(customerRepository, never()).linkSubject(any(), any());
    }

    @Test
    void doesNotReportAProfileThatAnotherUserAlreadyClaimed() {
        when(customerRepository.findBySubject("sub-2")).thenReturn(Optional.empty());
        when(customerRepository.findByEmail("alice@example.test")).thenReturn(Optional.of(EXISTING));
        when(customerRepository.linkSubject("10000001", "sub-2")).thenReturn(false);

        assertTrue(customerService.findMine("sub-2", "alice@example.test", true).isEmpty());
    }

    @Test
    void registeringTwiceIsRejected() {
        when(customerRepository.findBySubject("sub-1")).thenReturn(Optional.of(EXISTING));

        assertThrows(DuplicateCustomerException.class,
            () -> customerService.registerMine("sub-1", "alice@example.test", command(null, "+14155552671")));
        verify(customerRepository, never()).save(any(), any());
    }

    @Test
    void registersTheProfileForTheSubjectWithTheTokenEmail() {
        when(customerRepository.findBySubject("sub-1")).thenReturn(Optional.empty());
        when(customerRepository.save(any(), any())).thenReturn(EXISTING);

        customerService.registerMine("sub-1", "Alice@Example.test", command("ignored@example.test", "+14155552671"));

        verify(customerRepository).save(
            org.mockito.ArgumentMatchers.argThat(profile -> profile.email().equals("alice@example.test")),
            org.mockito.ArgumentMatchers.eq("sub-1"));
    }

    @Test
    void updateKeepsTheEmailAndRejectsAPhoneUsedByAnotherCustomer() {
        when(customerRepository.findBySubject("sub-1")).thenReturn(Optional.of(EXISTING));
        when(customerRepository.existsByPhoneExcluding("+14155552999", "10000001")).thenReturn(true);

        assertThrows(DuplicateCustomerException.class,
            () -> customerService.updateMine("sub-1", command("other@example.test", "+14155552999")));
        verify(customerRepository, never()).update(any());
    }

    @Test
    void updateWithoutAProfileIsNotFound() {
        when(customerRepository.findBySubject("sub-9")).thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class,
            () -> customerService.updateMine("sub-9", command(null, "+14155552671")));
    }
}
