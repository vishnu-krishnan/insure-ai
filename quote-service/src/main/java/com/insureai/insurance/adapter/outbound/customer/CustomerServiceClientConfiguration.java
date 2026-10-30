package com.insureai.insurance.adapter.outbound.customer;

import java.time.Duration;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

import com.insureai.insurance.application.port.outbound.CustomerProfileLookupPort;
import com.insureai.insurance.domain.exception.CustomerServiceUnavailableException;

@Configuration
public class CustomerServiceClientConfiguration {
    private static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String CORRELATION_MDC_KEY = "correlationId";

    @Bean
    OAuth2AuthorizedClientManager customerServiceAuthorizedClientManager(
        ClientRegistrationRepository registrations,
        OAuth2AuthorizedClientService authorizedClientService
    ) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClientService);
        manager.setAuthorizedClientProvider(new ClientCredentialsOAuth2AuthorizedClientProvider());
        return manager;
    }

    @Bean
    CustomerProfileLookupPort customerProfileLookupPort(
        @Value("${customer.service.base-url:http://localhost:8282}") String baseUrl,
        @Value("${customer.service.connect-timeout:1s}") Duration connectTimeout,
        @Value("${customer.service.read-timeout:2s}") Duration readTimeout,
        OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        RestClient restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .requestInterceptor((request, body, execution) -> {
                OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId("customer-service")
                    .principal("quote-service")
                    .build();
                OAuth2AuthorizedClient client = authorizedClientManager.authorize(authorizeRequest);
                if (client == null) {
                    throw new CustomerServiceUnavailableException(
                        "Could not obtain Customer Service access token",
                        null,
                        false
                    );
                }
                request.getHeaders().set(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + client.getAccessToken().getTokenValue()
                );
                String correlationId = MDC.get(CORRELATION_MDC_KEY);
                if (correlationId != null) {
                    request.getHeaders().set(CORRELATION_HEADER, correlationId);
                }
                return execution.execute(request, body);
            })
            .build();
        return new CustomerServiceHttpAdapter(restClient);
    }
}
