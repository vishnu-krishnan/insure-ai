package com.insureai.insurance.adapter.inbound.web;

import static com.insureai.insurance.QuoteFixtures.quote;
import static com.insureai.insurance.QuoteFixtures.withCreatedAt;
import static com.insureai.insurance.QuoteFixtures.withCreatedBy;
import static com.insureai.insurance.QuoteFixtures.withTypeDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.insureai.insurance.adapter.inbound.security.QuoteSecurityConfiguration;
import com.insureai.insurance.application.port.inbound.CreateCustomerQuoteUseCase;
import com.insureai.insurance.application.port.inbound.CreateQuoteCommand;
import com.insureai.insurance.application.port.inbound.CreateQuoteUseCase;
import com.insureai.insurance.application.port.inbound.GetQuoteUseCase;
import com.insureai.insurance.application.port.inbound.ListQuotesUseCase;
import com.insureai.insurance.application.port.inbound.QuotePage;
import com.insureai.insurance.domain.exception.DomainValidationException;
import com.insureai.insurance.domain.exception.QuoteNotFoundException;
import com.insureai.insurance.domain.model.CoverageType;
import com.insureai.insurance.domain.model.PolicyQuote;

@WebMvcTest(InsuranceQuoteController.class)
@Import(QuoteSecurityConfiguration.class)
@TestPropertySource(properties = {
    "security.jwt.issuer-uri=https://issuer.example/realms/test",
    "security.jwt.audience=quote-service",
    "spring.security.oauth2.client.registration.customer-service.client-id=test-quote-client",
    "spring.security.oauth2.client.registration.customer-service.client-secret=test-quote-secret",
    "spring.security.oauth2.client.provider.keycloak.token-uri=https://issuer.example/token"
})
class InsuranceQuoteControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateQuoteUseCase createQuoteUseCase;

    @MockitoBean
    private GetQuoteUseCase getQuoteUseCase;

    @MockitoBean
    private ListQuotesUseCase listQuotesUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void listQuotesUsesDefaultPagingAndReturnsPageMetadata() throws Exception {
        PolicyQuote quote = quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00", null, "QT-000012", "CUS-000003");
        when(listQuotesUseCase.listQuotes(0, 20, null, null)).thenReturn(new QuotePage(List.of(quote), 0, 20, 1));

        mockMvc.perform(get("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].quoteId").value("quote-1"))
            .andExpect(jsonPath("$.content[0].quoteNumber").value("QT-000012"))
            .andExpect(jsonPath("$.content[0].customerNumber").value("CUS-000003"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
        verify(listQuotesUseCase).listQuotes(0, 20, null, null);
    }

    @Test
    void listQuotesPassesRequestedPageAndSize() throws Exception {
        when(listQuotesUseCase.listQuotes(2, 5, null, null)).thenReturn(new QuotePage(List.of(), 2, 5, 11));

        mockMvc.perform(get("/api/v1/quotes")
                .param("page", "2")
                .param("size", "5")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void listQuotesPassesSearchTermToUseCase() throws Exception {
        when(listQuotesUseCase.listQuotes(0, 20, "alice", null)).thenReturn(new QuotePage(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/quotes")
                .param("q", "alice")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(0));
        verify(listQuotesUseCase).listQuotes(0, 20, "alice", null);
    }

    @Test
    void listQuotesReturnsProblemDetailForTooLongSearchTerm() throws Exception {
        String tooLong = "x".repeat(81);
        when(listQuotesUseCase.listQuotes(0, 20, tooLong, null))
            .thenThrow(new DomainValidationException("q must be at most 80 characters"));

        mockMvc.perform(get("/api/v1/quotes")
                .param("q", tooLong)
                .header("X-Correlation-Id", "trace-q")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUOTE_REQUEST"))
            .andExpect(jsonPath("$.correlationId").value("trace-q"))
            .andExpect(jsonPath("$.detail").value("q must be at most 80 characters"));
    }

    @Test
    void listQuotesReturnsProblemDetailForInvalidPaging() throws Exception {
        when(listQuotesUseCase.listQuotes(0, 1000, null, null))
            .thenThrow(new DomainValidationException("size must be between 1 and 100"));

        mockMvc.perform(get("/api/v1/quotes")
                .param("size", "1000")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUOTE_REQUEST"))
            .andExpect(jsonPath("$.detail").value("size must be between 1 and 100"));
    }

    @Test
    void listQuotesRejectsNonNumericPaging() throws Exception {
        mockMvc.perform(get("/api/v1/quotes")
                .param("page", "first")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(listQuotesUseCase);
    }

    @Test
    void listQuotesRequiresReadScope() throws Exception {
        mockMvc.perform(get("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create"))))
            .andExpect(status().isForbidden());
        verifyNoInteractions(listQuotesUseCase);
    }

    @Test
    void createQuoteReturnsCreatedResponseForValidRequest() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenReturn(quote("b9c76f7a-7281-4b40-9ebc-33cc99f3c3e8", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Alice Smith\",\"coverageType\":\"AUTO\",\"age\":35,\"coverageAmount\":250000}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerName").value("Alice Smith"))
            .andExpect(jsonPath("$.coverageType").value("AUTO"))
            .andExpect(jsonPath("$.annualPremium").isNumber());
        verify(createQuoteUseCase).createQuote(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createQuoteReturnsProblemDetailForInvalidInput() throws Exception {
        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"A\",\"coverageType\":\"AUTO\",\"age\":35,\"coverageAmount\":250000}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"));
        verifyNoInteractions(createQuoteUseCase);
    }

    @Test
    void persistenceFailuresReturnSafeProblemDetail() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenThrow(new DataAccessResourceFailureException("sensitive database details"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Alice Smith\",\"coverageType\":\"AUTO\",\"age\":35,\"coverageAmount\":250000}"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.title").value("Persistence failure"))
            .andExpect(jsonPath("$.detail").value("The quote could not be saved. Please try again later."));
    }

    @Test
    void getQuoteReturnsExplicitQuoteResponse() throws Exception {
        when(getQuoteUseCase.getQuoteById("quote-1"))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00", null, "QT-000001", null));

        mockMvc.perform(get("/api/v1/quotes/quote-1")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quoteId").value("quote-1"))
            .andExpect(jsonPath("$.quoteNumber").value("QT-000001"))
            .andExpect(jsonPath("$.customerNumber").doesNotExist())
            .andExpect(jsonPath("$.customerName").value("Alice Smith"))
            .andExpect(jsonPath("$.coverageType").value("AUTO"))
            .andExpect(jsonPath("$.annualPremium").value(17500.00))
            .andExpect(jsonPath("$.monthlyPremium").value(1458.33));
        verify(getQuoteUseCase).getQuoteById("quote-1");
    }

    @Test
    void getQuoteReturnsProblemDetailWhenQuoteDoesNotExist() throws Exception {
        when(getQuoteUseCase.getQuoteById("missing"))
            .thenThrow(new QuoteNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/quotes/missing")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Quote not found"))
            .andExpect(jsonPath("$.detail").value("Quote missing was not found"));
    }

    @Test
    void quoteCreationRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/quotes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Alice Smith\",\"coverageType\":\"AUTO\",\"age\":35,\"coverageAmount\":250000}"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(createQuoteUseCase);
    }

    @Test
    void quoteCreationRequiresCreateScope() throws Exception {
        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Alice Smith\",\"coverageType\":\"AUTO\",\"age\":35,\"coverageAmount\":250000}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(createQuoteUseCase);
    }

    @Test
    void unauthenticatedRequestsReceiveProblemDetailWithChallengeHeader() throws Exception {
        mockMvc.perform(get("/api/v1/quotes/quote-1"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().exists("WWW-Authenticate"))
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void errorResponsesCarryStableCodeAndCorrelationId() throws Exception {
        when(getQuoteUseCase.getQuoteById("missing"))
            .thenThrow(new QuoteNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/quotes/missing")
                .header("X-Correlation-Id", "trace-123")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isNotFound())
            .andExpect(header().string("X-Correlation-Id", "trace-123"))
            .andExpect(jsonPath("$.code").value("QUOTE_NOT_FOUND"))
            .andExpect(jsonPath("$.correlationId").value("trace-123"));
    }

    @Test
    void invalidCorrelationIdIsReplacedWithGeneratedValue() throws Exception {
        when(getQuoteUseCase.getQuoteById("missing"))
            .thenThrow(new QuoteNotFoundException("missing"));

        mockMvc.perform(get("/api/v1/quotes/missing")
                .header("X-Correlation-Id", "not valid!")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isNotFound())
            .andExpect(header().string("X-Correlation-Id", org.hamcrest.Matchers.not("not valid!")))
            .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void validationFailuresListEveryInvalidField() throws Exception {
        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"A\",\"coverageType\":\"AUTO\",\"age\":10,\"coverageAmount\":250000}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors.length()").value(2));
        verifyNoInteractions(createQuoteUseCase);
    }

    @Test
    void malformedJsonReturnsBadRequestProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(createQuoteUseCase);
    }

    @Test
    void unexpectedFailuresReturnSafeInternalErrorProblemDetail() throws Exception {
        when(getQuoteUseCase.getQuoteById("boom"))
            .thenThrow(new IllegalStateException("secret internal detail"));

        mockMvc.perform(get("/api/v1/quotes/boom")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."));
    }

    private static final String BASE_REQUEST =
        "\"customerName\":\"Alice Smith\",\"coverageType\":\"AUTO\",\"age\":35,\"coverageAmount\":250000";

    @Test
    void createQuoteWithoutNewFieldsStillReturnsCreatedWithDefaultsPassedAsNull() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + "}"))
            .andExpect(status().isCreated());

        CreateQuoteCommand command = captureCommand();
        assertNull(command.policyTermYears());
        assertNull(command.paymentFrequency());
        assertNull(command.deductible());
        assertNull(command.startDate());
    }

    @Test
    void createQuoteAcceptsPolicyDetailsAndPassesThemToUseCase() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + ",\"policyTermYears\":10,\"paymentFrequency\":\"MONTHLY\","
                    + "\"deductible\":500.25,\"startDate\":\"2026-12-01\"}"))
            .andExpect(status().isCreated());

        CreateQuoteCommand command = captureCommand();
        assertEquals(10, command.policyTermYears());
        assertEquals("MONTHLY", command.paymentFrequency());
        assertEquals(new BigDecimal("500.25"), command.deductible());
        assertEquals(LocalDate.of(2026, 12, 1), command.startDate());
    }

    @Test
    void createQuoteRecordsPreferredUsernameAsCreator() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create"))
                    .jwt(token -> token.claim("preferred_username", "agent.alice").subject("sub-1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + "}"))
            .andExpect(status().isCreated());

        assertEquals("agent.alice", captureCommand().createdBy());
    }

    @Test
    void createQuoteFallsBackToSubjectThenNullForCreator() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create"))
                    .jwt(token -> token.claim("preferred_username", " ").subject("sub-1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + "}"))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create"))
                    .jwt(token -> token.claims(claims -> {
                        claims.clear();
                        claims.put("scope", "quote:create");
                    })))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + "}"))
            .andExpect(status().isCreated());

        ArgumentCaptor<CreateQuoteCommand> captor = ArgumentCaptor.forClass(CreateQuoteCommand.class);
        verify(createQuoteUseCase, org.mockito.Mockito.times(2)).createQuote(captor.capture());
        assertEquals("sub-1", captor.getAllValues().get(0).createdBy());
        assertNull(captor.getAllValues().get(1).createdBy());
    }

    @Test
    void createQuoteRejectsInvalidPolicyDetailsNamingTheField() throws Exception {
        assertInvalidField("\"policyTermYears\":0", "policyTermYears");
        assertInvalidField("\"policyTermYears\":41", "policyTermYears");
        assertInvalidField("\"paymentFrequency\":\"WEEKLY\"", "paymentFrequency");
        assertInvalidField("\"deductible\":-1", "deductible");
        assertInvalidField("\"deductible\":10.123", "deductible");
        assertInvalidField("\"deductible\":1000000000000.00", "deductible");
        verifyNoInteractions(createQuoteUseCase);
    }

    @Test
    void createQuoteAcceptsLargestAllowedDeductibleShape() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + ",\"policyTermYears\":40,\"deductible\":999999999999.99}"))
            .andExpect(status().isCreated());
    }

    @Test
    void domainValidationWithFieldIsListedInErrors() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenThrow(new DomainValidationException("deductible must be less than coverageAmount", "deductible"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + ",\"deductible\":250000}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUOTE_REQUEST"))
            .andExpect(jsonPath("$.detail").value("deductible must be less than coverageAmount"))
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("deductible"))
            .andExpect(jsonPath("$.errors[0].message").value("deductible must be less than coverageAmount"));
    }

    @Test
    void domainValidationWithoutFieldHasNoErrorsList() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenThrow(new DomainValidationException("age must be between 18 and 75"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void getQuoteReturnsPolicyDetailsAndPremiumBreakdown() throws Exception {
        Instant createdAt = Instant.now().minusSeconds(60);
        when(getQuoteUseCase.getQuoteById("quote-1")).thenReturn(withCreatedBy(withCreatedAt(
            quote("quote-1", "Alice Smith", CoverageType.HOME, 35, "250000", "12500.00", null, "QT-000001", null),
            createdAt), "agent.alice"));

        mockMvc.perform(get("/api/v1/quotes/quote-1")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quoteNumber").value("QT-000001"))
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

    @Test
    void statusIsExpiredOncePastValidUntil() throws Exception {
        when(getQuoteUseCase.getQuoteById("old")).thenReturn(withCreatedAt(
            quote("old", "Alice Smith", CoverageType.HOME, 35, "250000", "12500.00"),
            Instant.parse("2020-01-01T00:00:00Z")));

        mockMvc.perform(get("/api/v1/quotes/old")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.validUntil").value("2020-01-31"))
            .andExpect(jsonPath("$.status").value("EXPIRED"))
            .andExpect(jsonPath("$.createdBy").value((Object) null));
    }

    @Test
    void listItemsCarryPolicyDetails() throws Exception {
        PolicyQuote monthly = quote("quote-1", "Alice Smith", CoverageType.HOME, 35, "250000", "12500.00");
        when(listQuotesUseCase.listQuotes(0, 20, null, null)).thenReturn(new QuotePage(List.of(monthly), 0, 20, 1));

        mockMvc.perform(get("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].policyTermYears").value(1))
            .andExpect(jsonPath("$.content[0].paymentFrequency").value("YEARLY"))
            .andExpect(jsonPath("$.content[0].deductible").value(0.0))
            .andExpect(jsonPath("$.content[0].startDate").value("2026-10-26"))
            .andExpect(jsonPath("$.content[0].validUntil").value("2026-11-25"))
            .andExpect(jsonPath("$.content[0].status").exists())
            .andExpect(jsonPath("$.content[0].taxRate").value(0.18))
            .andExpect(jsonPath("$.content[0].taxAmount").value(2250.00))
            .andExpect(jsonPath("$.content[0].totalAnnualPayable").value(14750.00))
            .andExpect(jsonPath("$.content[0].installmentAmount").value(14750.00))
            .andExpect(jsonPath("$.content[0].createdAt").value("2026-10-26T10:15:30.123456Z"))
            .andExpect(jsonPath("$.content[0].createdBy").value((Object) null));
    }

    private static final String HEALTH_DETAILS =
        "{\"members\":4,\"preExistingConditions\":false,\"smoker\":true,\"planTier\":\"STANDARD\"}";

    @Test
    void createQuoteAcceptsDetailsPassesThemToUseCaseAndReturnsThem() throws Exception {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("members", 4);
        details.put("preExistingConditions", false);
        details.put("smoker", true);
        details.put("planTier", "STANDARD");
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any())).thenReturn(withTypeDetails(
            quote("quote-1", "Alice Smith", CoverageType.HEALTH, 35, "250000", "20000.00"), details));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"Alice Smith\",\"coverageType\":\"HEALTH\",\"age\":35,"
                    + "\"coverageAmount\":250000,\"details\":" + HEALTH_DETAILS + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.details.members").value(4))
            .andExpect(jsonPath("$.details.preExistingConditions").value(false))
            .andExpect(jsonPath("$.details.smoker").value(true))
            .andExpect(jsonPath("$.details.planTier").value("STANDARD"));

        assertEquals(details, captureCommand().details());
    }

    @Test
    void createQuoteWithoutDetailsPassesNullAndReturnsNullDetails() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + "}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.details").value((Object) null));

        assertNull(captureCommand().details());
    }

    @Test
    void createQuoteWithExplicitNullDetailsIsAccepted() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any()))
            .thenReturn(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + ",\"details\":null}"))
            .andExpect(status().isCreated());

        assertNull(captureCommand().details());
    }

    @Test
    void invalidDetailsAreReportedAsFieldErrorNamedAfterTheDetailsKey() throws Exception {
        when(createQuoteUseCase.createQuote(org.mockito.ArgumentMatchers.any())).thenThrow(
            new DomainValidationException("details.vehicleYear must be between 1980 and 2027", "details.vehicleYear"));

        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + ",\"details\":{\"vehicleYear\":1900}}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUOTE_REQUEST"))
            .andExpect(jsonPath("$.errors[0].field").value("details.vehicleYear"))
            .andExpect(jsonPath("$.errors[0].message").value("details.vehicleYear must be between 1980 and 2027"));
    }

    @Test
    void detailsThatAreNotAJsonObjectAreRejected() throws Exception {
        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + ",\"details\":\"nope\"}"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(createQuoteUseCase);
    }

    @Test
    void getQuoteReturnsDetailsAndListItemsCarryThem() throws Exception {
        Map<String, Object> details = Map.of("propertyType", "VILLA", "builtYear", 2015, "areaSqFt", 2400,
            "ownership", "OWNED", "city", "Pune");
        PolicyQuote home = withTypeDetails(
            quote("quote-1", "Alice Smith", CoverageType.HOME, 35, "250000", "12500.00"), details);
        PolicyQuote plain = quote("quote-2", "Bob Jones", CoverageType.AUTO, 35, "250000", "17500.00");
        when(getQuoteUseCase.getQuoteById("quote-1")).thenReturn(home);
        when(getQuoteUseCase.getQuoteById("quote-2")).thenReturn(plain);
        when(listQuotesUseCase.listQuotes(0, 20, null, null)).thenReturn(new QuotePage(List.of(home, plain), 0, 20, 2));

        mockMvc.perform(get("/api/v1/quotes/quote-1")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.details.propertyType").value("VILLA"))
            .andExpect(jsonPath("$.details.builtYear").value(2015))
            .andExpect(jsonPath("$.details.areaSqFt").value(2400));
        mockMvc.perform(get("/api/v1/quotes/quote-2")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(jsonPath("$.details").value((Object) null));
        mockMvc.perform(get("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(jsonPath("$.content[0].details.city").value("Pune"))
            .andExpect(jsonPath("$.content[1].details").value((Object) null));
    }

    @Test
    void listQuotesPassesCoverageTypeParameterToUseCase() throws Exception {
        when(listQuotesUseCase.listQuotes(1, 5, "alice", "home")).thenReturn(new QuotePage(List.of(), 1, 5, 7));

        mockMvc.perform(get("/api/v1/quotes")
                .param("page", "1")
                .param("size", "5")
                .param("q", "alice")
                .param("coverageType", "home")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(7))
            .andExpect(jsonPath("$.totalPages").value(2));
        verify(listQuotesUseCase).listQuotes(1, 5, "alice", "home");
    }

    @Test
    void listQuotesReturnsProblemDetailForInvalidCoverageType() throws Exception {
        when(listQuotesUseCase.listQuotes(0, 20, null, "BOAT"))
            .thenThrow(new DomainValidationException("coverageType must be one of AUTO, HOME, LIFE, HEALTH"));

        mockMvc.perform(get("/api/v1/quotes")
                .param("coverageType", "BOAT")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_QUOTE_REQUEST"))
            .andExpect(jsonPath("$.detail").value("coverageType must be one of AUTO, HOME, LIFE, HEALTH"));
    }

    private CreateQuoteCommand captureCommand() {
        ArgumentCaptor<CreateQuoteCommand> captor = ArgumentCaptor.forClass(CreateQuoteCommand.class);
        verify(createQuoteUseCase).createQuote(captor.capture());
        return captor.getValue();
    }

    private void assertInvalidField(String extraJson, String field) throws Exception {
        mockMvc.perform(post("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_REQUEST + "," + extraJson + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[0].field").value(field));
    }
}
