package com.insureai.customer.adapter.inbound.web;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
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
import java.util.List;

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
import org.springframework.test.web.servlet.ResultActions;

import com.insureai.customer.adapter.inbound.security.CustomerSecurityConfiguration;
import com.insureai.customer.application.port.inbound.CreateCustomerCommand;
import com.insureai.customer.application.port.inbound.CreateCustomerUseCase;
import com.insureai.customer.application.port.inbound.CustomerPage;
import com.insureai.customer.application.port.inbound.GetCustomerUseCase;
import com.insureai.customer.application.port.inbound.SearchCustomersUseCase;
import com.insureai.customer.domain.exception.CustomerNotFoundException;
import com.insureai.customer.domain.exception.CustomerValidationException;
import com.insureai.customer.domain.exception.DuplicateCustomerException;
import com.insureai.customer.domain.model.Address;
import com.insureai.customer.domain.model.CustomerProfile;
import com.insureai.customer.domain.model.Gender;
import com.insureai.customer.domain.model.MaritalStatus;
import com.insureai.customer.domain.model.Nominee;

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
    private SearchCustomersUseCase searchCustomersUseCase;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void createCustomerRequiresCustomerCreateScope() throws Exception {
        when(createCustomerUseCase.createCustomer(any()))
            .thenReturn(new CustomerProfile(
                "3b241101-e2bb-4255-8caf-4136c566a962",
                "CUS-000001",
                "Alice Smith",
                "alice@example.test",
                "+14155552671",
                LocalDate.of(1990, 1, 1),
                null, null, null, null, null, null, null, null
            ));

        mockMvc.perform(post("/api/v1/customers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"fullName":"Alice Smith","email":"alice@example.test","phone":"+14155552671","dateOfBirth":"1990-01-01"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value("3b241101-e2bb-4255-8caf-4136c566a962"))
            .andExpect(jsonPath("$.customerNumber").value("CUS-000001"))
            .andExpect(jsonPath("$.email").value("alice@example.test"));
        verify(createCustomerUseCase).createCustomer(any(CreateCustomerCommand.class));
    }

    @Test
    void duplicateEmailReturnsConflictProblemDetail() throws Exception {
        when(createCustomerUseCase.createCustomer(any())).thenThrow(new DuplicateCustomerException("email"));

        postCustomer("")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.title").value("Duplicate customer"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("A customer with this email already exists"))
            .andExpect(jsonPath("$.code").value("DUPLICATE_CUSTOMER_EMAIL"))
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("email"))
            .andExpect(jsonPath("$.errors[0].message").value("A customer with this email already exists"));
    }

    @Test
    void duplicatePhoneReturnsConflictProblemDetail() throws Exception {
        when(createCustomerUseCase.createCustomer(any())).thenThrow(new DuplicateCustomerException("phone"));

        postCustomer("")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.detail").value("A customer with this phone number already exists"))
            .andExpect(jsonPath("$.code").value("DUPLICATE_CUSTOMER_PHONE"))
            .andExpect(jsonPath("$.errors[0].field").value("phone"))
            .andExpect(jsonPath("$.errors[0].message").value("A customer with this phone number already exists"));
    }

    @Test
    void rejectsFullNameWithDigitsOrSymbols() throws Exception {
        mockMvc.perform(post("/api/v1/customers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:create")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + BASE_FIELDS.replace("Alice Smith", "Alice 2<b>") + "}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[?(@.field == 'fullName')]").isNotEmpty());
        verifyNoInteractions(createCustomerUseCase);
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
                "CUS-000001",
                "Alice Smith",
                "alice@example.test",
                "+14155552671",
                LocalDate.of(1990, 1, 1),
                null, null, null, null, null, null, null, null
            ));

        mockMvc.perform(get("/api/v1/customers/3b241101-e2bb-4255-8caf-4136c566a962")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.customerNumber").value("CUS-000001"))
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

    @Test
    void searchCustomersReturnsPageForAuthorizedCaller() throws Exception {
        CustomerProfile alice = new CustomerProfile(
            "3b241101-e2bb-4255-8caf-4136c566a962",
            "CUS-000001",
            "Alice Smith",
            "alice@example.test",
            "+14155552671",
            LocalDate.of(1990, 1, 1),
            null, null, null, null, null, null, null, null
        );
        when(searchCustomersUseCase.searchCustomers("alice", 1, 5))
            .thenReturn(new CustomerPage(List.of(alice), 1, 5, 6));

        mockMvc.perform(get("/api/v1/customers")
                .param("q", "alice")
                .param("page", "1")
                .param("size", "5")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].customerNumber").value("CUS-000001"))
            .andExpect(jsonPath("$.content[0].fullName").value("Alice Smith"))
            .andExpect(jsonPath("$.page").value(1))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.totalElements").value(6))
            .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void searchCustomersDefaultsToFirstPageOfTwenty() throws Exception {
        when(searchCustomersUseCase.searchCustomers(null, 0, 20))
            .thenReturn(new CustomerPage(List.of(), 0, 20, 0));

        mockMvc.perform(get("/api/v1/customers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));
        verify(searchCustomersUseCase).searchCustomers(null, 0, 20);
    }

    @Test
    void searchCustomersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/customers"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(searchCustomersUseCase);
    }

    @Test
    void searchCustomersRequiresCustomerReadScope() throws Exception {
        mockMvc.perform(get("/api/v1/customers")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:create"))))
            .andExpect(status().isForbidden());
        verifyNoInteractions(searchCustomersUseCase);
    }

    @Test
    void searchCustomersMapsValidationFailureToProblemDetail() throws Exception {
        when(searchCustomersUseCase.searchCustomers(eq("x".repeat(81)), eq(0), eq(20)))
            .thenThrow(new CustomerValidationException("q must be at most 80 characters"));

        mockMvc.perform(get("/api/v1/customers")
                .param("q", "x".repeat(81))
                .header("X-Correlation-Id", "trace-789")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_CUSTOMER_REQUEST"))
            .andExpect(jsonPath("$.correlationId").value("trace-789"));
    }

    @Test
    void searchCustomersRejectsOutOfRangeSize() throws Exception {
        when(searchCustomersUseCase.searchCustomers(null, 0, 0))
            .thenThrow(new CustomerValidationException("size must be between 1 and 100"));
        when(searchCustomersUseCase.searchCustomers(null, 0, 101))
            .thenThrow(new CustomerValidationException("size must be between 1 and 100"));

        for (String size : List.of("0", "101")) {
            mockMvc.perform(get("/api/v1/customers")
                    .param("size", size)
                    .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_CUSTOMER_REQUEST"));
        }
    }

    private static final String BASE_FIELDS =
        "\"fullName\":\"Alice Smith\",\"email\":\"alice@example.test\",\"phone\":\"+14155552671\",\"dateOfBirth\":\"1990-01-01\"";
    private static final String VALID_ADDRESS =
        "{\"line1\":\"1 Main St\",\"city\":\"Pune\",\"state\":\"MH\",\"postalCode\":\"411 001\",\"country\":\"IN\"}";

    private static CustomerProfile detailedCustomer() {
        return new CustomerProfile(
            "3b241101-e2bb-4255-8caf-4136c566a962", "CUS-000001", "Alice Smith", "alice@example.test", "+14155552671",
            LocalDate.of(1990, 1, 1), Gender.FEMALE, MaritalStatus.MARRIED, "Engineer", new BigDecimal("52000.50"),
            new Address("1 Main St", "Flat 2", "Pune", "MH", "411001", "IN"), new Nominee("Nina Rao", "Spouse"),
            Instant.parse("2026-10-27T10:15:30.123456Z"), Instant.parse("2026-10-27T10:15:30.123456Z")
        );
    }

    private ResultActions postCustomer(String extraFields) throws Exception {
        return mockMvc.perform(post("/api/v1/customers")
            .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:create")))
            .contentType(MediaType.APPLICATION_JSON)
            .content("{" + BASE_FIELDS + (extraFields.isEmpty() ? "" : "," + extraFields) + "}"));
    }

    private void assertRejectedField(String extraFields, String field) throws Exception {
        postCustomer(extraFields)
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.errors[?(@.field == '" + field + "')]").isNotEmpty());
        verifyNoInteractions(createCustomerUseCase);
    }

    @Test
    void createCustomerAcceptsFullDetailsAndReturnsThem() throws Exception {
        when(createCustomerUseCase.createCustomer(any())).thenReturn(detailedCustomer());

        postCustomer("""
            "gender":"FEMALE","maritalStatus":"MARRIED","occupation":"  Engineer ","annualIncome":52000.50,
            "address":{"line1":"1 Main St","line2":"Flat 2","city":"Pune","state":"MH","postalCode":"411001","country":"in"},
            "nominee":{"name":"Nina Rao","relationship":"Spouse"}
            """)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.gender").value("FEMALE"))
            .andExpect(jsonPath("$.maritalStatus").value("MARRIED"))
            .andExpect(jsonPath("$.occupation").value("Engineer"))
            .andExpect(jsonPath("$.annualIncome").value(52000.50))
            .andExpect(jsonPath("$.address.line1").value("1 Main St"))
            .andExpect(jsonPath("$.address.line2").value("Flat 2"))
            .andExpect(jsonPath("$.address.postalCode").value("411001"))
            .andExpect(jsonPath("$.address.country").value("IN"))
            .andExpect(jsonPath("$.nominee.name").value("Nina Rao"))
            .andExpect(jsonPath("$.nominee.relationship").value("Spouse"))
            .andExpect(jsonPath("$.createdAt").value("2026-10-27T10:15:30.123456Z"))
            .andExpect(jsonPath("$.updatedAt").value("2026-10-27T10:15:30.123456Z"));

        ArgumentCaptor<CreateCustomerCommand> captor = ArgumentCaptor.forClass(CreateCustomerCommand.class);
        verify(createCustomerUseCase).createCustomer(captor.capture());
        CreateCustomerCommand command = captor.getValue();
        assertEquals(Gender.FEMALE, command.gender());
        assertEquals(MaritalStatus.MARRIED, command.maritalStatus());
        assertEquals("Engineer", command.occupation());
        assertEquals(new BigDecimal("52000.50"), command.annualIncome());
        assertEquals("IN", command.address().country());
        assertEquals(new Nominee("Nina Rao", "Spouse"), command.nominee());
    }

    @Test
    void createCustomerWithoutNewFieldsStillSucceeds() throws Exception {
        CustomerProfile plain = new CustomerProfile(
            "3b241101-e2bb-4255-8caf-4136c566a962", "CUS-000001", "Alice Smith", "alice@example.test", "+14155552671",
            LocalDate.of(1990, 1, 1), null, null, null, null, null, null,
            Instant.parse("2026-10-27T10:15:30Z"), Instant.parse("2026-10-27T10:15:30Z")
        );
        when(createCustomerUseCase.createCustomer(any())).thenReturn(plain);

        postCustomer("")
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerNumber").value("CUS-000001"))
            .andExpect(jsonPath("$.gender").doesNotExist())
            .andExpect(jsonPath("$.address").doesNotExist())
            .andExpect(jsonPath("$.nominee").doesNotExist())
            .andExpect(jsonPath("$.createdAt").value("2026-10-27T10:15:30Z"));

        ArgumentCaptor<CreateCustomerCommand> captor = ArgumentCaptor.forClass(CreateCustomerCommand.class);
        verify(createCustomerUseCase).createCustomer(captor.capture());
        assertEquals(null, captor.getValue().address());
        assertEquals(null, captor.getValue().gender());
    }

    @Test
    void createCustomerNormalisesLowerCaseCountry() throws Exception {
        when(createCustomerUseCase.createCustomer(any())).thenReturn(detailedCustomer());

        postCustomer("\"address\":" + VALID_ADDRESS.replace("\"IN\"", "\"in\"")).andExpect(status().isCreated());

        ArgumentCaptor<CreateCustomerCommand> captor = ArgumentCaptor.forClass(CreateCustomerCommand.class);
        verify(createCustomerUseCase).createCustomer(captor.capture());
        assertEquals("IN", captor.getValue().address().country());
    }

    @Test
    void createCustomerAcceptsAddressWithoutLineTwo() throws Exception {
        when(createCustomerUseCase.createCustomer(any())).thenReturn(detailedCustomer());

        postCustomer("\"address\":" + VALID_ADDRESS).andExpect(status().isCreated());
    }

    @Test
    void getCustomerReturnsDetails() throws Exception {
        when(getCustomerUseCase.getCustomerById("3b241101-e2bb-4255-8caf-4136c566a962")).thenReturn(detailedCustomer());

        mockMvc.perform(get("/api/v1/customers/3b241101-e2bb-4255-8caf-4136c566a962")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.gender").value("FEMALE"))
            .andExpect(jsonPath("$.address.city").value("Pune"))
            .andExpect(jsonPath("$.nominee.relationship").value("Spouse"))
            .andExpect(jsonPath("$.createdAt").value("2026-10-27T10:15:30.123456Z"));
    }

    @Test
    void searchItemsContainDetails() throws Exception {
        when(searchCustomersUseCase.searchCustomers("pune", 0, 20))
            .thenReturn(new CustomerPage(List.of(detailedCustomer()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/customers")
                .param("q", "pune")
                .with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_customer:read"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].occupation").value("Engineer"))
            .andExpect(jsonPath("$.content[0].annualIncome").value(52000.50))
            .andExpect(jsonPath("$.content[0].address.postalCode").value("411001"))
            .andExpect(jsonPath("$.content[0].updatedAt").value("2026-10-27T10:15:30.123456Z"));
    }

    @Test
    void rejectsUnknownGender() throws Exception {
        assertRejectedField("\"gender\":\"ROBOT\"", "gender");
    }

    @Test
    void rejectsUnknownMaritalStatus() throws Exception {
        assertRejectedField("\"maritalStatus\":\"COMPLICATED\"", "maritalStatus");
    }

    @Test
    void rejectsOverlongOccupation() throws Exception {
        assertRejectedField("\"occupation\":\"" + "x".repeat(81) + "\"", "occupation");
    }

    @Test
    void rejectsNegativeAnnualIncome() throws Exception {
        assertRejectedField("\"annualIncome\":-1", "annualIncome");
    }

    @Test
    void rejectsAnnualIncomeWithTooManyDigits() throws Exception {
        assertRejectedField("\"annualIncome\":1000000000000", "annualIncome");
        assertRejectedField("\"annualIncome\":10.123", "annualIncome");
    }

    @Test
    void rejectsAddressMissingRequiredParts() throws Exception {
        List<String> parts = List.of("line1", "city", "state", "postalCode", "country");
        for (String missing : parts) {
            String address = parts.stream()
                .filter(part -> !part.equals(missing))
                .map(part -> "\"" + part + "\":\"AB1\"")
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
            assertRejectedField("\"address\":" + address, "address." + missing);
        }
        assertRejectedField("\"address\":{}", "address.line1");
    }

    @Test
    void rejectsMalformedAddressParts() throws Exception {
        assertRejectedField("\"address\":" + VALID_ADDRESS.replace("1 Main St", "x".repeat(121)), "address.line1");
        assertRejectedField("\"address\":" + VALID_ADDRESS.replace("\"city\"", "\"line2\":\"" + "x".repeat(121) + "\",\"city\""), "address.line2");
        assertRejectedField("\"address\":" + VALID_ADDRESS.replace("Pune", "x".repeat(81)), "address.city");
        assertRejectedField("\"address\":" + VALID_ADDRESS.replace("\"MH\"", "\"" + "x".repeat(81) + "\""), "address.state");
        assertRejectedField("\"address\":" + VALID_ADDRESS.replace("411 001", "41"), "address.postalCode");
        assertRejectedField("\"address\":" + VALID_ADDRESS.replace("411 001", "1234567890123"), "address.postalCode");
        assertRejectedField("\"address\":" + VALID_ADDRESS.replace("411 001", "411#01"), "address.postalCode");
        assertRejectedField("\"address\":" + VALID_ADDRESS.replace("\"IN\"", "\"IND\""), "address.country");
        assertRejectedField("\"address\":" + VALID_ADDRESS.replace("\"IN\"", "\"I1\""), "address.country");
    }

    @Test
    void rejectsNomineeMissingRequiredParts() throws Exception {
        assertRejectedField("\"nominee\":{\"relationship\":\"Spouse\"}", "nominee.name");
        assertRejectedField("\"nominee\":{\"name\":\"Nina Rao\"}", "nominee.relationship");
    }

    @Test
    void rejectsMalformedNomineeParts() throws Exception {
        assertRejectedField("\"nominee\":{\"name\":\"N\",\"relationship\":\"Spouse\"}", "nominee.name");
        assertRejectedField("\"nominee\":{\"name\":\" N \",\"relationship\":\"Spouse\"}", "nominee.name");
        assertRejectedField("\"nominee\":{\"name\":\"" + "x".repeat(81) + "\",\"relationship\":\"Spouse\"}", "nominee.name");
        assertRejectedField("\"nominee\":{\"name\":\"Nina Rao\",\"relationship\":\"" + "x".repeat(41) + "\"}", "nominee.relationship");
    }
}
