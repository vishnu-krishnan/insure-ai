package com.insureai.customer.application.port.inbound;

import java.util.Optional;

import com.insureai.customer.domain.model.CustomerProfile;

/** Self-service profile for the signed-in customer, identified by their Keycloak subject. */
public interface MyProfileUseCase {
    /**
     * Finds the profile owned by the subject. When none is linked yet and the email is verified, an unlinked
     * profile registered by an agent under the same email is claimed.
     */
    Optional<CustomerProfile> findMine(String subject, String email, boolean emailVerified);

    CustomerProfile registerMine(String subject, String email, CreateCustomerCommand command);

    CustomerProfile updateMine(String subject, CreateCustomerCommand command);

    Optional<CustomerProfile> findBySubject(String subject);
}
