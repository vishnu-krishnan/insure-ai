package com.insureai.insurance.adapter.inbound.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.insureai.insurance.application.port.inbound.CreateQuoteUseCase;
import com.insureai.insurance.domain.model.PolicyQuote;

@RestController
@RequestMapping("/api/v1")
public class InsuranceQuoteController {
    private final CreateQuoteUseCase createQuoteUseCase;

    public InsuranceQuoteController(CreateQuoteUseCase createQuoteUseCase) {
        this.createQuoteUseCase = createQuoteUseCase;
    }

    @PostMapping("/quotes")
    public ResponseEntity<QuoteResponseDto> createQuote(@Valid @RequestBody QuoteRequestDto request) {
        PolicyQuote quote = createQuoteUseCase.createQuote(request.toCommand());
        return ResponseEntity.status(HttpStatus.CREATED).body(QuoteResponseDto.fromDomain(quote));
    }
}
