package com.insureai.customer.application.port.outbound;

import java.util.List;
import java.util.Optional;

import com.insureai.customer.domain.model.CustomerProfile;

public interface CustomerRepositoryPort {
    CustomerProfile save(CustomerProfile customer);

    /** Saves a new profile owned by the given Keycloak subject. */
    CustomerProfile save(CustomerProfile customer, String subject);

    /** Overwrites the editable fields of an existing profile; the email and IDs never change. */
    CustomerProfile update(CustomerProfile customer);

    Optional<CustomerProfile> findBySubject(String subject);

    Optional<CustomerProfile> findByEmail(String email);

    /** Links an unlinked profile to the subject; false when it is missing or already linked. */
    boolean linkSubject(String customerId, String subject);

    boolean existsByPhoneExcluding(String phone, String customerId);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<CustomerProfile> findById(String customerId);

    List<CustomerProfile> search(String text, long offset, int limit);

    long countSearch(String text);
}
