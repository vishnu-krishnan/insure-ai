package com.insureai.customer.adapter.inbound.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.insureai.customer.application.port.inbound.CreateCustomerUseCase;
import com.insureai.customer.application.port.inbound.GetCustomerUseCase;
import com.insureai.customer.application.port.inbound.SearchCustomersUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customers", description = "Customer profile creation, retrieval and search")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "403", description = "The token does not carry the required scope",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "500", description = "Persistence or unexpected failure",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
public class CustomerController {
    private final CreateCustomerUseCase createCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final SearchCustomersUseCase searchCustomersUseCase;

    public CustomerController(
        CreateCustomerUseCase createCustomerUseCase,
        GetCustomerUseCase getCustomerUseCase,
        SearchCustomersUseCase searchCustomersUseCase
    ) {
        this.createCustomerUseCase = createCustomerUseCase;
        this.getCustomerUseCase = getCustomerUseCase;
        this.searchCustomersUseCase = searchCustomersUseCase;
    }

    @PostMapping
    @Operation(summary = "Create a customer profile", description = "Required scope: customer:create.")
    @ApiResponse(responseCode = "201", description = "Customer created")
    @ApiResponse(responseCode = "400", description = "Validation failed",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CustomerResponseDto> createCustomer(
        @Valid @RequestBody CreateCustomerRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(CustomerResponseDto.fromDomain(createCustomerUseCase.createCustomer(request.toCommand())));
    }

    @GetMapping
    @Operation(
        summary = "Search customer profiles",
        description = "Required scope: customer:read. Case-insensitive contains match on name, email, phone, "
            + "customer number, customer ID, occupation, city and postal code. Results are ordered newest first and never exceed 100 per page."
    )
    @ApiResponse(responseCode = "400", description = "Invalid search text or paging parameters",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public CustomerPageResponseDto searchCustomers(
        @Parameter(description = "Text to look for, at most 80 characters; blank returns all customers")
        @RequestParam(name = "q", required = false) String q,
        @Parameter(description = "Zero-based page index") @RequestParam(name = "page", defaultValue = "0") int page,
        @Parameter(description = "Page size, between 1 and 100") @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return CustomerPageResponseDto.fromDomain(searchCustomersUseCase.searchCustomers(q, page, size));
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get a customer profile by ID", description = "Required scope: customer:read.")
    @ApiResponse(responseCode = "400", description = "customerId is not a valid UUID",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Customer not found",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public CustomerResponseDto getCustomer(@PathVariable("customerId") String customerId) {
        return CustomerResponseDto.fromDomain(getCustomerUseCase.getCustomerById(customerId));
    }
}
