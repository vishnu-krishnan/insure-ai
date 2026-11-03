package com.insureai.insurance.adapter.inbound.web;

import java.time.Clock;
import java.time.LocalDate;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.insureai.insurance.application.port.inbound.MyQuotesUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/me/quotes")
@Tag(name = "My quotes", description = "Quotes for the signed-in customer")
public class MyQuoteController {
    private final MyQuotesUseCase myQuotesUseCase;
    private final Clock clock;

    public MyQuoteController(MyQuotesUseCase myQuotesUseCase, Clock clock) {
        this.myQuotesUseCase = myQuotesUseCase;
        this.clock = clock;
    }

    @GetMapping
    @Operation(summary = "List my quotes", description = "Required scope: me:read. Newest first.")
    public QuotePageResponseDto listMine(
        @AuthenticationPrincipal Jwt jwt,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return QuotePageResponseDto.fromDomain(myQuotesUseCase.listMine(jwt.getSubject(), page, size), LocalDate.now(clock));
    }

    @GetMapping("/{quoteId}")
    @Operation(summary = "Get one of my quotes", description = "Required scope: me:read. Quotes of other customers are reported as not found.")
    @ApiResponse(responseCode = "404", description = "Quote not found",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public QuoteResponseDto getMine(@AuthenticationPrincipal Jwt jwt, @PathVariable("quoteId") String quoteId) {
        return QuoteResponseDto.fromDomain(myQuotesUseCase.getMine(jwt.getSubject(), quoteId), LocalDate.now(clock));
    }

    @PostMapping
    @Operation(summary = "Create a quote for myself", description = "Required scope: me:quote. Name and age come from my profile.")
    @ApiResponse(responseCode = "201", description = "Quote created")
    @ApiResponse(responseCode = "409", description = "Profile not completed yet",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<QuoteResponseDto> createMine(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody MyQuoteRequestDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(QuoteResponseDto.fromDomain(
            myQuotesUseCase.createMine(jwt.getSubject(), request.toCommand(QuoteCreator.from(jwt))),
            LocalDate.now(clock)));
    }
}
