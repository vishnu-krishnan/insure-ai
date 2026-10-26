package com.insureai.insurance.adapter.outbound.customer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import com.sun.net.httpserver.HttpServer;

class CustomerServiceClientConfigurationTest {
    @Test
    void sendsClientCredentialsAccessTokenToCustomerService() throws Exception {
        AtomicReference<String> authorizationHeader = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/customers/customer-123", exchange -> {
            authorizationHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            byte[] response = """
                {"customerId":"customer-123","fullName":"Alice Smith","dateOfBirth":"1990-01-01"}
                """.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var responseBody = exchange.getResponseBody()) {
                responseBody.write(response);
            }
        });
        server.start();

        try {
            ClientRegistration registration = ClientRegistration.withRegistrationId("customer-service")
                .clientId("quote-service")
                .clientSecret("test-secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://identity.test/token")
                .build();
            OAuth2AccessToken token = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "test-access-token",
                Instant.now(),
                Instant.now().plusSeconds(300)
            );
            OAuth2AuthorizedClient authorizedClient =
                new OAuth2AuthorizedClient(registration, "quote-service", token);
            OAuth2AuthorizedClientManager authorizedClientManager = mock(OAuth2AuthorizedClientManager.class);
            when(authorizedClientManager.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient);

            CustomerServiceClientConfiguration configuration = new CustomerServiceClientConfiguration();
            var customerLookup = configuration.customerProfileLookupPort(
                "http://127.0.0.1:" + server.getAddress().getPort(),
                Duration.ofSeconds(1),
                Duration.ofSeconds(1),
                authorizedClientManager
            );

            customerLookup.findCustomer("customer-123");

            assertEquals("Bearer test-access-token", authorizationHeader.get());
        } finally {
            server.stop(0);
        }
    }
}
