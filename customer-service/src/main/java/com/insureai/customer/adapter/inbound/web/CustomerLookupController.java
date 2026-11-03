package com.insureai.customer.adapter.inbound.web;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.insureai.customer.application.port.inbound.MyProfileUseCase;
import com.insureai.customer.domain.exception.CustomerNotFoundException;
import com.insureai.customer.domain.exception.CustomerValidationException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

/** Service-to-service lookup used by Quote Service to find the customer behind a signed-in user. */
@RestController
@RequestMapping("/api/v1/customers/by-subject")
@Tag(name = "Customers", description = "Customer profile creation, retrieval and search")
public class CustomerLookupController {
    private final MyProfileUseCase myProfileUseCase;

    public CustomerLookupController(MyProfileUseCase myProfileUseCase) {
        this.myProfileUseCase = myProfileUseCase;
    }

    @GetMapping("/{subject}")
    @Operation(summary = "Get a customer profile by Keycloak subject", description = "Required scope: customer:read.")
    @ApiResponse(responseCode = "404", description = "No customer is linked to this subject",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public CustomerResponseDto getBySubject(@PathVariable("subject") String subject) {
        if (!subject.matches("^[A-Za-z0-9-]{1,64}$")) {
            throw new CustomerValidationException("subject is not valid");
        }
        return myProfileUseCase.findBySubject(subject)
            .map(CustomerResponseDto::fromDomain)
            .orElseThrow(() -> new CustomerNotFoundException("for subject " + subject));
    }
}
