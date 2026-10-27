package com.insureai.insurance.adapter.inbound.web;

import static com.insureai.insurance.QuoteFixtures.quote;
import static com.insureai.insurance.QuoteFixtures.withCreatedAt;
import static com.insureai.insurance.QuoteFixtures.withCreatedBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00", CUSTOMER_ID, "QT-000005", "CUS-000002"));

        mockMvc.perform(post("/api/v2/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"customerId":"3b241101-e2bb-4255-8caf-4136c566a962","coverageType":"AUTO","coverageAmount":250000}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value(CUSTOMER_ID))
            .andExpect(jsonPath("$.quoteNumber").value("QT-000005"))
            .andExpect(jsonPath("$.customerNumber").value("CUS-000002"))
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

    @Test
    void requestWithoutNewFieldsPassesNullDetailsToUseCase() throws Exception {
        when(createCustomerQuoteUseCase.createQuoteForCustomer(any()))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v2/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + CUSTOMER_ID + "\",\"coverageType\":\"AUTO\",\"coverageAmount\":250000}"))
            .andExpect(status().isCreated());

        CreateCustomerQuoteCommand command = captureCommand();
        assertNull(command.policyTermYears());
        assertNull(command.paymentFrequency());
        assertNull(command.deductible());
        assertNull(command.startDate());
    }

    @Test
    void acceptsPolicyDetailsAndRecordsCreator() throws Exception {
        when(createCustomerQuoteUseCase.createQuoteForCustomer(any()))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v2/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create"))
                    .jwt(token -> token.claim("preferred_username", "agent.alice")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + CUSTOMER_ID + "\",\"coverageType\":\"AUTO\",\"coverageAmount\":250000,"
                    + "\"policyTermYears\":20,\"paymentFrequency\":\"QUARTERLY\",\"deductible\":1000,"
                    + "\"startDate\":\"2026-11-01\"}"))
            .andExpect(status().isCreated());

        CreateCustomerQuoteCommand command = captureCommand();
        assertEquals(20, command.policyTermYears());
        assertEquals("QUARTERLY", command.paymentFrequency());
        assertEquals(new BigDecimal("1000"), command.deductible());
        assertEquals(LocalDate.of(2026, 11, 1), command.startDate());
        assertEquals("agent.alice", command.createdBy());
    }

    @Test
    void usesSubjectWhenPreferredUsernameIsMissing() throws Exception {
        when(createCustomerQuoteUseCase.createQuoteForCustomer(any()))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v2/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create"))
                    .jwt(token -> token.subject("sub-9")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + CUSTOMER_ID + "\",\"coverageType\":\"AUTO\",\"coverageAmount\":250000}"))
            .andExpect(status().isCreated());

        assertEquals("sub-9", captureCommand().createdBy());
    }

    @Test
    void rejectsInvalidPolicyDetailsNamingTheField() throws Exception {
        assertInvalidField("\"policyTermYears\":41", "policyTermYears");
        assertInvalidField("\"policyTermYears\":0", "policyTermYears");
        assertInvalidField("\"paymentFrequency\":\"DAILY\"", "paymentFrequency");
        assertInvalidField("\"deductible\":-5", "deductible");
        assertInvalidField("\"deductible\":1.005", "deductible");
        verifyNoInteractions(createCustomerQuoteUseCase);
    }

    @Test
    void responseContainsPolicyDetailsAndPremiumBreakdown() throws Exception {
        Instant createdAt = Instant.now();
        when(createCustomerQuoteUseCase.createQuoteForCustomer(any())).thenReturn(withCreatedBy(withCreatedAt(
            quote("quote-1", "Alice Smith", CoverageType.HOME, 35, "250000", "12500.00", CUSTOMER_ID,
                "QT-000005", "CUS-000002"), createdAt), "agent.alice"));

        mockMvc.perform(post("/api/v2/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + CUSTOMER_ID + "\",\"coverageType\":\"HOME\",\"coverageAmount\":250000}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.quoteNumber").value("QT-000005"))
            .andExpect(jsonPath("$.customerNumber").value("CUS-000002"))
            .andExpect(jsonPath("$.annualPremium").value(12500.00))
            .andExpect(jsonPath("$.monthlyPremium").value(1041.67))
            .andExpect(jsonPath("$.policyTermYears").value(1))
            .andExpect(jsonPath("$.paymentFrequency").value("YEARLY"))
            .andExpect(jsonPath("$.deductible").value(0.0))
            .andExpect(jsonPath("$.startDate").value("2026-10-26"))
            .andExpect(jsonPath("$.validUntil").value(
                LocalDate.ofInstant(createdAt, java.time.ZoneOffset.UTC).plusDays(30).toString()))
            .andExpect(jsonPath("$.status").value("ISSUED"))
            .andExpect(jsonPath("$.taxRate").value(0.18))
            .andExpect(jsonPath("$.taxAmount").value(2250.00))
            .andExpect(jsonPath("$.totalAnnualPayable").value(14750.00))
            .andExpect(jsonPath("$.installmentAmount").value(14750.00))
            .andExpect(jsonPath("$.createdAt").value(createdAt.toString()))
            .andExpect(jsonPath("$.createdBy").value("agent.alice"));
    }

    private CreateCustomerQuoteCommand captureCommand() {
        ArgumentCaptor<CreateCustomerQuoteCommand> captor = ArgumentCaptor.forClass(CreateCustomerQuoteCommand.class);
        verify(createCustomerQuoteUseCase).createQuoteForCustomer(captor.capture());
        return captor.getValue();
    }

    private void assertInvalidField(String extraJson, String field) throws Exception {
        mockMvc.perform(post("/api/v2/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerId\":\"" + CUSTOMER_ID + "\",\"coverageType\":\"AUTO\",\"coverageAmount\":250000,"
                    + extraJson + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value(field));
    }
}
