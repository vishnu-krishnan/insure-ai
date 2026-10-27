package com.insureai.insurance.adapter.inbound.web;

import java.time.Clock;
import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.insureai.insurance.application.port.inbound.CreateCustomerQuoteUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v2/quotes")
@Tag(name = "Customer quotes", description = "Quotes created for an existing customer")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "403", description = "The token does not carry the required scope",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "500", description = "Persistence or unexpected failure",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
public class CustomerQuoteController {
    private final CreateCustomerQuoteUseCase createCustomerQuoteUseCase;
    private final Clock clock;

    public CustomerQuoteController(CreateCustomerQuoteUseCase createCustomerQuoteUseCase, Clock clock) {
        this.createCustomerQuoteUseCase = createCustomerQuoteUseCase;
        this.clock = clock;
    }

    @PostMapping
    @Operation(
        summary = "Create a quote for an existing customer",
        description = "Required scope: quote:create. Name and age are taken from the customer profile held by "
            + "Customer Service."
    )
    @ApiResponse(responseCode = "201", description = "Quote created")
    @ApiResponse(responseCode = "400", description = "Validation failed or unsupported coverage type",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "The customer does not exist",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "503", description = "Customer Service is unavailable",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "504", description = "Customer Service timed out",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CustomerQuoteResponseDto> createQuote(
        @Valid @RequestBody CreateCustomerQuoteRequestDto request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(CustomerQuoteResponseDto.fromDomain(
                createCustomerQuoteUseCase.createQuoteForCustomer(request.toCommand(QuoteCreator.from(jwt))),
                LocalDate.now(clock)
            ));
    }
}
