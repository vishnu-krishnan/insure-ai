package com.insureai.customer.adapter.inbound.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

/**
 * The signed-in customer's own profile. The owner is always the token subject, never an ID from the request,
 * so a customer cannot reach anyone else's data through these endpoints.
 */
@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "My profile", description = "Self-service profile for the signed-in customer")
public class MeController {
    private final MyProfileUseCase myProfileUseCase;

    public MeController(MyProfileUseCase myProfileUseCase) {
        this.myProfileUseCase = myProfileUseCase;
    }

    @GetMapping
    @Operation(summary = "Get my profile", description = "Required scope: me:read. 404 until the customer has completed their profile.")
    @ApiResponse(responseCode = "404", description = "No profile yet",
        content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    public CustomerResponseDto getMine(@AuthenticationPrincipal Jwt jwt) {
        return myProfileUseCase.findMine(jwt.getSubject(), jwt.getClaimAsString("email"), verified(jwt))
            .map(CustomerResponseDto::forOwner)
            .orElseThrow(() -> new CustomerNotFoundException("me"));
    }

    @PostMapping
    @Operation(summary = "Create my profile", description = "Required scope: me:write. The email comes from the verified token.")
    @ApiResponse(responseCode = "201", description = "Profile created")
    public ResponseEntity<CustomerResponseDto> registerMine(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody MyProfileRequestDto request
    ) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank() || !verified(jwt)) {
            throw new CustomerValidationException("a verified email address is required to create a profile");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(CustomerResponseDto.forOwner(
                myProfileUseCase.registerMine(jwt.getSubject(), email, request.toCommand())));
    }

    @PutMapping
    @Operation(summary = "Update my profile", description = "Required scope: me:write. The email cannot be changed here.")
    public CustomerResponseDto updateMine(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody MyProfileRequestDto request
    ) {
        return CustomerResponseDto.forOwner(myProfileUseCase.updateMine(jwt.getSubject(), request.toCommand()));
    }

    private static boolean verified(Jwt jwt) {
        return Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
    }
}
