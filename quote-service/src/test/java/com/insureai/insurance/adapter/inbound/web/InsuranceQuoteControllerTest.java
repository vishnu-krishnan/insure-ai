package com.insureai.insurance.adapter.inbound.web;

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
import java.util.List;

import org.junit.jupiter.api.Test;
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
        PolicyQuote quote = new PolicyQuote(
            "quote-1",
            "Alice Smith",
            CoverageType.AUTO,
            35,
            new BigDecimal("250000"),
            new BigDecimal("17500.00")
        );
        when(listQuotesUseCase.listQuotes(0, 20)).thenReturn(new QuotePage(List.of(quote), 0, 20, 1));

        mockMvc.perform(get("/api/v1/quotes")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].quoteId").value("quote-1"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(20))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.totalPages").value(1));
        verify(listQuotesUseCase).listQuotes(0, 20);
    }

    @Test
    void listQuotesPassesRequestedPageAndSize() throws Exception {
        when(listQuotesUseCase.listQuotes(2, 5)).thenReturn(new QuotePage(List.of(), 2, 5, 11));

        mockMvc.perform(get("/api/v1/quotes")
                .param("page", "2")
                .param("size", "5")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void listQuotesReturnsProblemDetailForInvalidPaging() throws Exception {
        when(listQuotesUseCase.listQuotes(0, 1000))
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
            .thenReturn(new PolicyQuote(
                "b9c76f7a-7281-4b40-9ebc-33cc99f3c3e8",
                "Alice Smith",
                CoverageType.AUTO,
                35,
                new BigDecimal("250000"),
                new BigDecimal("17500.00")
            ));

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
            .thenReturn(new PolicyQuote(
                "quote-1",
                "Alice Smith",
                CoverageType.AUTO,
                35,
                new BigDecimal("250000"),
                new BigDecimal("17500.00")
            ));

        mockMvc.perform(get("/api/v1/quotes/quote-1")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quoteId").value("quote-1"))
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
}
