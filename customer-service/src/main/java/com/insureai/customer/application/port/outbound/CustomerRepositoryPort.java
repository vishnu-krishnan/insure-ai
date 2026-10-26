package com.insureai.customer.application.port.outbound;

import java.util.Optional;

import com.insureai.customer.domain.model.CustomerProfile;

public interface CustomerRepositoryPort {
    void save(CustomerProfile customer);

    Optional<CustomerProfile> findById(String customerId);
}
