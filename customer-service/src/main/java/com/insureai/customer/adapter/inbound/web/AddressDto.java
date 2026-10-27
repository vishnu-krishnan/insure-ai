package com.insureai.customer.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.insureai.customer.domain.model.Address;

public record AddressDto(
    @NotBlank(message = "address.line1 is required")
    @Size(max = 120, message = "address.line1 must not exceed 120 characters")
    String line1,

    @Size(max = 120, message = "address.line2 must not exceed 120 characters")
    String line2,

    @NotBlank(message = "address.city is required")
    @Size(max = 80, message = "address.city must not exceed 80 characters")
    String city,

    @NotBlank(message = "address.state is required")
    @Size(max = 80, message = "address.state must not exceed 80 characters")
    String state,

    @NotBlank(message = "address.postalCode is required")
    @Pattern(
        regexp = "^[A-Za-z0-9-][A-Za-z0-9 -]{1,10}[A-Za-z0-9-]$",
        message = "address.postalCode must be 3 to 12 letters, digits, spaces or hyphens"
    )
    String postalCode,

    @NotBlank(message = "address.country is required")
    @Pattern(regexp = "^[A-Za-z]{2}$", message = "address.country must be an ISO 3166-1 alpha-2 code")
    String country
) {
    public Address toDomain() {
        return new Address(line1, line2, city, state, postalCode, country);
    }

    public static AddressDto fromDomain(Address address) {
        return address == null ? null : new AddressDto(
            address.line1(),
            address.line2(),
            address.city(),
            address.state(),
            address.postalCode(),
            address.country()
        );
    }
}
