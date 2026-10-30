package com.insureai.insurance.adapter.outbound.customer;

import java.net.SocketTimeoutException;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.insureai.insurance.application.port.outbound.CustomerProfileLookupPort;
import com.insureai.insurance.application.port.outbound.CustomerProfileSnapshot;
import com.insureai.insurance.domain.exception.CustomerReferenceNotFoundException;
import com.insureai.insurance.domain.exception.CustomerServiceTimeoutException;
import com.insureai.insurance.domain.exception.CustomerServiceUnavailableException;

public class CustomerServiceHttpAdapter implements CustomerProfileLookupPort {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerServiceHttpAdapter.class);
    private final RestClient restClient;

    public CustomerServiceHttpAdapter(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public CustomerProfileSnapshot findCustomer(String customerId) {
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                CustomerServiceResponseDto customer = restClient.get()
                    .uri("/api/v1/customers/{customerId}", customerId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (request, response) -> {
                        throw new CustomerReferenceNotFoundException(customerId);
                    })
                    .onStatus(status -> status.value() == 504, (request, response) -> {
                        throw new CustomerServiceTimeoutException(
                            "Customer Service timed out",
                            null
                        );
                    })
                    .onStatus(status -> status.value() == 502 || status.value() == 503 || status.value() == 504,
                        (request, response) -> {
                        throw new CustomerServiceUnavailableException(
                            "Customer Service returned a server error",
                            null,
                            true
                        );
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                        throw new CustomerServiceUnavailableException(
                            "Customer Service returned a non-retryable server error",
                            null,
                            false
                        );
                    })
                    .body(CustomerServiceResponseDto.class);

                if (customer == null || customer.customerId() == null
                    || customer.fullName() == null || customer.dateOfBirth() == null) {
                    throw new CustomerServiceUnavailableException(
                        "Customer Service returned an invalid profile response",
                        null,
                        false
                    );
                }
                return new CustomerProfileSnapshot(
                    customer.customerId(),
                    customer.fullName(),
                    customer.dateOfBirth(),
                    customer.customerNumber()
                );
            } catch (CustomerReferenceNotFoundException exception) {
                throw exception;
            } catch (CustomerServiceTimeoutException exception) {
                if (attempt == 1) {
                    throw exception;
                }
                LOGGER.warn("Customer Service lookup timed out; retrying (customerId={}).", customerId);
            } catch (CustomerServiceUnavailableException exception) {
                if (!exception.retryable() || attempt == 1) {
                    throw exception;
                }
                LOGGER.warn("Customer Service lookup failed ({}); retrying (customerId={}).", exception.getMessage(), customerId);
            } catch (HttpClientErrorException exception) {
                LOGGER.warn(
                    "Customer Service rejected the lookup with status {} (customerId={}).",
                    exception.getStatusCode().value(), customerId
                );
                throw new CustomerServiceUnavailableException(
                    "Customer Service rejected the service request",
                    exception,
                    false
                );
            } catch (OAuth2AuthorizationException exception) {
                LOGGER.error("Could not obtain a Customer Service access token ({}).", exception.getError().getErrorCode());
                throw new CustomerServiceUnavailableException(
                    "Could not obtain Customer Service access token",
                    exception,
                    false
                );
            } catch (ResourceAccessException exception) {
                if (attempt == 1) {
                    if (hasTimeoutCause(exception)) {
                        throw new CustomerServiceTimeoutException("Customer Service timed out", exception);
                    }
                    throw new CustomerServiceUnavailableException("Customer Service is unavailable", exception, true);
                }
                LOGGER.warn("Customer Service unreachable; retrying (customerId={}).", customerId);
            }
        }

        throw new IllegalStateException("Customer lookup retry loop exited unexpectedly");
    }

    private static boolean hasTimeoutCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }
}
