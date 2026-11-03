package com.insureai.customer.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.insureai.customer.adapter.inbound.security.CustomerSecurityConfiguration;
import com.insureai.customer.application.port.inbound.CreateCustomerUseCase;
import com.insureai.customer.application.port.inbound.CustomerPage;
import com.insureai.customer.application.port.inbound.GetCustomerUseCase;
import com.insureai.customer.application.port.inbound.MyProfileUseCase;
import com.insureai.customer.application.port.inbound.SearchCustomersUseCase;
import com.insureai.customer.domain.model.CustomerProfile;

@WebMvcTest({CustomerController.class, MeController.class})
@Import(CustomerSecurityConfiguration.class)
@TestPropertySource(properties = {
    "security.jwt.issuer-uri=https://issuer.example/realms/test",
    "security.jwt.audience=customer-service"
})
class MeAccessControlTest {
    private static final CustomerProfile ME = new CustomerProfile(
        "10000001", "CUS-000001", "Alice Smith", "alice@example.test", "+14155552671",
        LocalDate.of(1990, 1, 1), null, null, null, null, null, null, null, null
    );

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCustomerUseCase createCustomerUseCase;

    @MockitoBean
    private GetCustomerUseCase getCustomerUseCase;

    @MockitoBean
    private SearchCustomersUseCase searchCustomersUseCase;

    @MockitoBean
    private MyProfileUseCase myProfileUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    /** A token issued to the web client, as a signed-in customer or agent would hold. */
    private static JwtRequestPostProcessor webToken(String... authorities) {
        return jwt()
            .jwt(token -> token.subject("user-1").claim("azp", "insure-ui").claim("email", "alice@example.test")
                .claim("email_verified", true))
            .authorities(java.util.Arrays.stream(authorities)
                .map(name -> (org.springframework.security.core.GrantedAuthority) new SimpleGrantedAuthority(name))
                .toArray(org.springframework.security.core.GrantedAuthority[]::new));
    }

    @Test
    void aCustomerHoldingAnAgentScopeStillCannotUseAgentApis() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                .with(webToken("SCOPE_customer:read", "ROLE_customer")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(searchCustomersUseCase);
    }

    @Test
    void anAgentUsingTheWebClientCanUseAgentApis() throws Exception {
        when(searchCustomersUseCase.searchCustomers(any(), eq(0), eq(20)))
            .thenReturn(new CustomerPage(List.of(ME), 0, 20, 1));

        mockMvc.perform(get("/api/v1/customers")
                .with(webToken("SCOPE_customer:read", "ROLE_customer", "ROLE_agent")))
            .andExpect(status().isOk());
    }

    @Test
    void aServiceClientNeedsOnlyTheScope() throws Exception {
        when(searchCustomersUseCase.searchCustomers(any(), eq(0), eq(20)))
            .thenReturn(new CustomerPage(List.of(ME), 0, 20, 1));

        mockMvc.perform(get("/api/v1/customers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
            .andExpect(status().isOk());
    }

    @Test
    void myProfileNeedsTheCustomerRoleAndTheMeScope() throws Exception {
        mockMvc.perform(get("/api/v1/me").with(webToken("SCOPE_me:read")))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/me").with(webToken("ROLE_customer")))
            .andExpect(status().isForbidden());
        verifyNoInteractions(myProfileUseCase);
    }

    @Test
    void myProfileIsLookedUpByTheTokenSubjectOnly() throws Exception {
        when(myProfileUseCase.findMine("user-1", "alice@example.test", true)).thenReturn(Optional.of(ME));

        mockMvc.perform(get("/api/v1/me").with(webToken("SCOPE_me:read", "ROLE_customer")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerId").value("10000001"));
    }

    @Test
    void myProfileIsNotFoundBeforeItIsCompleted() throws Exception {
        when(myProfileUseCase.findMine("user-1", "alice@example.test", true)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/me").with(webToken("SCOPE_me:read", "ROLE_customer")))
            .andExpect(status().isNotFound());
    }

    @Test
    void creatingMyProfileRequiresAVerifiedEmail() throws Exception {
        mockMvc.perform(post("/api/v1/me")
                .with(jwt()
                    .jwt(token -> token.subject("user-2").claim("email", "bob@example.test").claim("email_verified", false))
                    .authorities(new SimpleGrantedAuthority("SCOPE_me:write"), new SimpleGrantedAuthority("ROLE_customer")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Bob Jones","phone":"+14155552672","dateOfBirth":"1990-01-01"}
                    """))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(myProfileUseCase);
    }
}
