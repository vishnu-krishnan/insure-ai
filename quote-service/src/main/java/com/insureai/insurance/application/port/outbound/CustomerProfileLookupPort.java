package com.insureai.insurance.application.port.outbound;

import java.util.Optional;

public interface CustomerProfileLookupPort {
    CustomerProfileSnapshot findCustomer(String customerId);

    /** The customer linked to a Keycloak subject, or empty when that user has no profile yet. */
    Optional<CustomerProfileSnapshot> findCustomerBySubject(String subject);
}
