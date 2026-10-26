package com.insureai.insurance.adapter.inbound.web;

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

import com.insureai.insurance.application.port.inbound.CreateQuoteUseCase;
import com.insureai.insurance.application.port.inbound.GetQuoteUseCase;
import com.insureai.insurance.application.port.inbound.ListQuotesUseCase;
import com.insureai.insurance.domain.model.PolicyQuote;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Quotes", description = "Quote creation and retrieval")
@ApiResponses({
    @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "403", description = "The token does not carry the required scope",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class))),
    @ApiResponse(responseCode = "500", description = "Persistence or unexpected failure",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
})
public class InsuranceQuoteController {
    private final CreateQuoteUseCase createQuoteUseCase;
    private final GetQuoteUseCase getQuoteUseCase;
    private final ListQuotesUseCase listQuotesUseCase;

    public InsuranceQuoteController(
        CreateQuoteUseCase createQuoteUseCase,
        GetQuoteUseCase getQuoteUseCase,
        ListQuotesUseCase listQuotesUseCase
    ) {
        this.createQuoteUseCase = createQuoteUseCase;
        this.getQuoteUseCase = getQuoteUseCase;
        this.listQuotesUseCase = listQuotesUseCase;
    }

    @GetMapping("/quotes")
    @Operation(
        summary = "List quotes",
        description = "Required scope: quote:read. Results are ordered by quoteId and never exceed 100 per page."
    )
    @ApiResponse(responseCode = "400", description = "Invalid paging parameters",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public QuotePageResponseDto listQuotes(
        @Parameter(description = "Zero-based page index") @RequestParam(name = "page", defaultValue = "0") int page,
        @Parameter(description = "Page size, between 1 and 100") @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return QuotePageResponseDto.fromDomain(listQuotesUseCase.listQuotes(page, size));
    }

    @PostMapping("/quotes")
    @Operation(
        summary = "Create a quote",
        description = "Required scope: quote:create. Customer details are supplied in the request."
    )
    @ApiResponse(responseCode = "201", description = "Quote created")
    @ApiResponse(responseCode = "400", description = "Validation failed or unsupported coverage type",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<QuoteResponseDto> createQuote(@Valid @RequestBody QuoteRequestDto request) {
        PolicyQuote quote = createQuoteUseCase.createQuote(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(QuoteResponseDto.fromDomain(quote));
    }

    @GetMapping("/quotes/{quoteId}")
    @Operation(summary = "Get a quote by ID", description = "Required scope: quote:read.")
    @ApiResponse(responseCode = "404", description = "Quote not found",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public QuoteResponseDto getQuote(@PathVariable("quoteId") String quoteId) {
        return QuoteResponseDto.fromDomain(getQuoteUseCase.getQuoteById(quoteId));
    }
}
