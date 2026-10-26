package com.insureai.insurance.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.insureai.insurance.adapter.inbound.security.QuoteSecurityConfiguration;
import com.insureai.insurance.application.port.inbound.CreateCustomerQuoteCommand;
import com.insureai.insurance.application.port.inbound.CreateCustomerQuoteUseCase;
import com.insureai.insurance.domain.exception.CustomerReferenceNotFoundException;
import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PolicyQuote;

@WebMvcTest(CustomerQuoteController.class)
@Import(QuoteSecurityConfiguration.class)
@TestPropertySource(properties = {
    "security.jwt.issuer-uri=https://issuer.example/realms/test",
    "security.jwt.audience=quote-service",
    "spring.security.oauth2.client.registration.customer-service.client-id=test-quote-client",
    "spring.security.oauth2.client.registration.customer-service.client-secret=test-quote-secret",
    "spring.security.oauth2.client.provider.keycloak.token-uri=https://issuer.example/token"
})
class CustomerQuoteControllerTest {
    private static final String CUSTOMER_ID = "3b241101-e2bb-4255-8caf-4136c566a962";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCustomerQuoteUseCase createCustomerQuoteUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createsCustomerReferencedQuoteForAuthorizedRequest() throws Exception {
        when(createCustomerQuoteUseCase.createQuoteForCustomer(any()))
            .thenReturn(new PolicyQuote(
                "quote-1",
                "Alice Smith",
                CoverageType.AUTO,
                35,
                new BigDecimal("250000"),
                new BigDecimal("17500.00"),
                CUSTOMER_ID
            ));

        mockMvc.perform(post("/api/v2/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId":"3b241101-e2bb-4255-8caf-4136c566a962","coverageType":"AUTO","coverageAmount":250000}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
            .andExpect(jsonPath("$.customerName").value("Alice Smith"))
            .andExpect(jsonPath("$.annualPremium").value(17500.00));
        verify(createCustomerQuoteUseCase).createQuoteForCustomer(any(CreateCustomerQuoteCommand.class));
    }

    @Test
    void invalidCustomerIdIsRejectedBeforeUseCase() throws Exception {
        mockMvc.perform(post("/api/v2/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId":"not-a-uuid","coverageType":"AUTO","coverageAmount":250000}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"));
        verifyNoInteractions(createCustomerQuoteUseCase);
    }

    @Test
    void unknownCustomerReturnsUnprocessableEntity() throws Exception {
        when(createCustomerQuoteUseCase.createQuoteForCustomer(any()))
            .thenThrow(new CustomerReferenceNotFoundException(CUSTOMER_ID));

        mockMvc.perform(post("/api/v2/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId":"3b241101-e2bb-4255-8caf-4136c566a962","coverageType":"AUTO","coverageAmount":250000}
                    """))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.title").value("Invalid customer reference"));
    }
}
