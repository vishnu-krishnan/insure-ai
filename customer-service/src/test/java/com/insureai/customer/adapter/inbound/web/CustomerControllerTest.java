package com.insureai.customer.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

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

import com.insureai.customer.adapter.inbound.security.CustomerSecurityConfiguration;
import com.insureai.customer.application.port.inbound.CreateCustomerCommand;
import com.insureai.customer.application.port.inbound.CreateCustomerUseCase;
import com.insureai.customer.application.port.inbound.GetCustomerUseCase;
import com.insureai.customer.domain.exception.CustomerNotFoundException;
import com.insureai.customer.domain.model.CustomerProfile;

@WebMvcTest(CustomerController.class)
@Import(CustomerSecurityConfiguration.class)
@TestPropertySource(properties = {
    "security.jwt.issuer-uri=https://issuer.example/realms/test",
    "security.jwt.audience=customer-service"
})
class CustomerControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateCustomerUseCase createCustomerUseCase;

    @MockitoBean
    private GetCustomerUseCase getCustomerUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createCustomerRequiresCustomerCreateScope() throws Exception {
        when(createCustomerUseCase.createCustomer(any()))
            .thenReturn(new CustomerProfile(
                "3b241101-e2bb-4255-8caf-4136c566a962",
                "Alice Smith",
                "alice@example.test",
                "+14155552671",
                LocalDate.of(1990, 1, 1)
            ));

        mockMvc.perform(post("/api/v1/customers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Alice Smith","email":"alice@example.test","phone":"+14155552671","dateOfBirth":"1990-01-01"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value("3b241101-e2bb-4255-8caf-4136c566a962"))
            .andExpect(jsonPath("$.email").value("alice@example.test"));
        verify(createCustomerUseCase).createCustomer(any(CreateCustomerCommand.class));
    }

    @Test
    void rejectsUnauthenticatedCustomerRequest() throws Exception {
        mockMvc.perform(get("/api/v1/customers/3b241101-e2bb-4255-8caf-4136c566a962"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(getCustomerUseCase);
    }

    @Test
    void rejectsCustomerRequestWithoutRequiredScope() throws Exception {
        mockMvc.perform(get("/api/v1/customers/3b241101-e2bb-4255-8caf-4136c566a962")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isForbidden());
        verifyNoInteractions(getCustomerUseCase);
    }

    @Test
    void getCustomerReturnsProfileForAuthorizedCaller() throws Exception {
        when(getCustomerUseCase.getCustomerById("3b241101-e2bb-4255-8caf-4136c566a962"))
            .thenReturn(new CustomerProfile(
                "3b241101-e2bb-4255-8caf-4136c566a962",
                "Alice Smith",
                "alice@example.test",
                "+14155552671",
                LocalDate.of(1990, 1, 1)
            ));

        mockMvc.perform(get("/api/v1/customers/3b241101-e2bb-4255-8caf-4136c566a962")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fullName").value("Alice Smith"))
            .andExpect(jsonPath("$.dateOfBirth").value("1990-01-01"));
        verify(getCustomerUseCase).getCustomerById("3b241101-e2bb-4255-8caf-4136c566a962");
    }

    @Test
    void validationErrorsUseProblemDetails() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"A","email":"bad","phone":"123","dateOfBirth":"2999-01-01"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation failed"))
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors.length()").value(4));
        verifyNoInteractions(createCustomerUseCase);
    }

    @Test
    void unauthenticatedRequestsReceiveProblemDetailWithChallengeHeader() throws Exception {
        mockMvc.perform(get("/api/v1/customers/3b241101-e2bb-4255-8caf-4136c566a962"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().exists("WWW-Authenticate"))
            .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void forbiddenRequestsReceiveProblemDetail() throws Exception {
        mockMvc.perform(get("/api/v1/customers/3b241101-e2bb-4255-8caf-4136c566a962")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_quote:read"))))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void missingCustomerReturnsStableCodeAndEchoesCorrelationId() throws Exception {
        when(getCustomerUseCase.getCustomerById("3b241101-e2bb-4255-8caf-4136c566a962"))
            .thenThrow(new CustomerNotFoundException("3b241101-e2bb-4255-8caf-4136c566a962"));

        mockMvc.perform(get("/api/v1/customers/3b241101-e2bb-4255-8caf-4136c566a962")
                .header("X-Correlation-Id", "trace-456")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
            .andExpect(status().isNotFound())
            .andExpect(header().string("X-Correlation-Id", "trace-456"))
            .andExpect(jsonPath("$.code").value("CUSTOMER_NOT_FOUND"))
            .andExpect(jsonPath("$.correlationId").value("trace-456"));
    }

    @Test
    void malformedJsonReturnsBadRequestProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"));
        verifyNoInteractions(createCustomerUseCase);
    }

    @Test
    void unexpectedFailuresReturnSafeInternalErrorProblemDetail() throws Exception {
        when(getCustomerUseCase.getCustomerById("3b241101-e2bb-4255-8caf-4136c566a962"))
            .thenThrow(new IllegalStateException("secret internal detail"));

        mockMvc.perform(get("/api/v1/customers/3b241101-e2bb-4255-8caf-4136c566a962")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
            .andExpect(jsonPath("$.detail").value("An unexpected error occurred. Please try again later."));
    }
}
