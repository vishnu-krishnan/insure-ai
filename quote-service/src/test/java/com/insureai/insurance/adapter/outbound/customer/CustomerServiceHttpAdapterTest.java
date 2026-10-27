package com.insureai.insurance.adapter.outbound.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.insureai.insurance.application.port.outbound.CustomerProfileSnapshot;
import com.insureai.insurance.domain.exception.CustomerReferenceNotFoundException;
import com.insureai.insurance.domain.exception.CustomerServiceUnavailableException;

class CustomerServiceHttpAdapterTest {
    private static final String CUSTOMER_ID = "3b241101-e2bb-4255-8caf-4136c566a962";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private CustomerServiceHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder().baseUrl("http://customer.test");
        server = MockRestServiceServer.bindTo(builder).build();
        adapter = new CustomerServiceHttpAdapter(builder.build());
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @Test
    void readsVersionedCustomerApiResponse() {
        server.expect(requestTo("http://customer.test/api/v1/customers/" + CUSTOMER_ID))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess("""
                {"customerId":"3b241101-e2bb-4255-8caf-4136c566a962","customerNumber":"CUS-000001",
                 "fullName":"Alice Smith","email":"alice@example.test","phone":"+14155552671",
                 "dateOfBirth":"1990-01-01","unknownField":"ignored"}
                """, MediaType.APPLICATION_JSON));

        assertEquals(
            new CustomerProfileSnapshot(CUSTOMER_ID, "Alice Smith", LocalDate.of(1990, 1, 1), "CUS-000001"),
            adapter.findCustomer(CUSTOMER_ID)
        );
    }

    @Test
    void treatsMissingCustomerNumberAsNull() {
        server.expect(requestTo("http://customer.test/api/v1/customers/" + CUSTOMER_ID))
            .andRespond(withSuccess("""
                {"customerId":"3b241101-e2bb-4255-8caf-4136c566a962","fullName":"Alice Smith",
                 "email":"alice@example.test","phone":"+14155552671","dateOfBirth":"1990-01-01"}
                """, MediaType.APPLICATION_JSON));

        assertNull(adapter.findCustomer(CUSTOMER_ID).customerNumber());
    }

    @Test
    void doesNotRetryUnknownCustomer() {
        server.expect(requestTo("http://customer.test/api/v1/customers/" + CUSTOMER_ID))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThrows(CustomerReferenceNotFoundException.class, () -> adapter.findCustomer(CUSTOMER_ID));
    }

    @Test
    void retriesTransientUnavailableResponseOnce() {
        server.expect(org.springframework.test.web.client.ExpectedCount.twice(),
                requestTo("http://customer.test/api/v1/customers/" + CUSTOMER_ID))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(CustomerServiceUnavailableException.class, () -> adapter.findCustomer(CUSTOMER_ID));
    }

    @Test
    void doesNotRetryNonTransientServerError() {
        server.expect(requestTo("http://customer.test/api/v1/customers/" + CUSTOMER_ID))
            .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThrows(CustomerServiceUnavailableException.class, () -> adapter.findCustomer(CUSTOMER_ID));
    }
}
