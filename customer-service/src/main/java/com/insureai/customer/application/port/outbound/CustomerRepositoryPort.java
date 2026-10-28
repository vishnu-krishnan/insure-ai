package com.insureai.customer.application.port.outbound;

import java.util.List;
import java.util.Optional;

import com.insureai.customer.domain.model.CustomerProfile;

public interface CustomerRepositoryPort {
    CustomerProfile save(CustomerProfile customer);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Optional<CustomerProfile> findById(String customerId);

    List<CustomerProfile> search(String text, long offset, int limit);

    long countSearch(String text);
}
