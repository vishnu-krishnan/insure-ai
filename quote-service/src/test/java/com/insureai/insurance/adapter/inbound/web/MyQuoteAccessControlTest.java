package com.insureai.insurance.adapter.inbound.web;

import static com.insureai.insurance.QuoteFixtures.quote;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.insureai.insurance.adapter.inbound.security.QuoteSecurityConfiguration;
import com.insureai.insurance.application.port.inbound.CreateQuoteUseCase;
import com.insureai.insurance.application.port.inbound.GetQuoteUseCase;
import com.insureai.insurance.application.port.inbound.ListQuotesUseCase;
import com.insureai.insurance.application.port.inbound.MyQuotesUseCase;
import com.insureai.insurance.application.port.inbound.QuotePage;
import com.insureai.insurance.domain.exception.CustomerProfileMissingException;
import com.insureai.insurance.domain.exception.QuoteNotFoundException;
import com.insureai.insurance.domain.model.CoverageType;

@WebMvcTest({InsuranceQuoteController.class, MyQuoteController.class})
@Import({QuoteSecurityConfiguration.class, MyQuoteAccessControlTest.ClockConfig.class})
@TestPropertySource(properties = {
    "security.jwt.issuer-uri=https://issuer.example/realms/test",
    "security.jwt.audience=quote-service",
    "spring.security.oauth2.client.registration.customer-service.client-id=test-quote-client",
    "spring.security.oauth2.client.registration.customer-service.client-secret=test-quote-secret",
    "spring.security.oauth2.client.provider.keycloak.token-uri=https://issuer.example/token"
})
class MyQuoteAccessControlTest {
    @TestConfiguration
    static class ClockConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateQuoteUseCase createQuoteUseCase;

    @MockitoBean
    private GetQuoteUseCase getQuoteUseCase;

    @MockitoBean
    private ListQuotesUseCase listQuotesUseCase;

    @MockitoBean
    private MyQuotesUseCase myQuotesUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static JwtRequestPostProcessor webToken(String... authorities) {
        return jwt()
            .jwt(token -> token.subject("user-1").claim("azp", "insure-ui"))
            .authorities(Arrays.stream(authorities)
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority(name))
                .toArray(GrantedAuthority[]::new));
    }

    @Test
    void aCustomerCannotReadTheAgentQuoteList() throws Exception {
        mockMvc.perform(get("/api/v1/quotes").with(webToken("SCOPE_quote:read", "ROLE_customer")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(listQuotesUseCase);
    }

    @Test
    void aCustomerCannotReadAnotherQuoteThroughTheAgentEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/quotes/some-quote").with(webToken("SCOPE_quote:read", "ROLE_customer")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(getQuoteUseCase);
    }

    @Test
    void myQuotesNeedTheCustomerRoleAndTheMeScope() throws Exception {
        mockMvc.perform(get("/api/v1/me/quotes").with(webToken("ROLE_customer")))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/me/quotes").with(webToken("SCOPE_me:read")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(myQuotesUseCase);
    }

    @Test
    void myQuotesAreListedForTheTokenSubject() throws Exception {
        when(myQuotesUseCase.listMine("user-1", 0, 20)).thenReturn(new QuotePage(
            List.of(quote("quote-1", "Alice Smith", CoverageType.AUTO, 35, "250000", "17500.00")), 0, 20, 1));

        mockMvc.perform(get("/api/v1/me/quotes").with(webToken("SCOPE_me:read", "ROLE_customer")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].quoteId").value("quote-1"));
    }

    @Test
    void someoneElsesQuoteIsReportedAsNotFound() throws Exception {
        when(myQuotesUseCase.getMine("user-1", "not-mine")).thenThrow(new QuoteNotFoundException("not-mine"));

        mockMvc.perform(get("/api/v1/me/quotes/not-mine").with(webToken("SCOPE_me:read", "ROLE_customer")))
            .andExpect(status().isNotFound());
    }

    @Test
    void requestingAQuoteBeforeCompletingTheProfileIsRejected() throws Exception {
        when(myQuotesUseCase.createMine(eq("user-1"), any())).thenThrow(new CustomerProfileMissingException());

        mockMvc.perform(post("/api/v1/me/quotes")
                .with(webToken("SCOPE_me:quote", "ROLE_customer"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"coverageType":"AUTO","coverageAmount":250000,"details":{"vehicleMake":"Honda"}}
                    """))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("PROFILE_REQUIRED"));
    }
}
