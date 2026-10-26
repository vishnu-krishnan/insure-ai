package com.insureai.customer.application.port.inbound;

import com.insureai.customer.domain.model.CustomerProfile;

public interface GetCustomerUseCase {
    CustomerProfile getCustomerById(String customerId);
}
