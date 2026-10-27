package com.insureai.customer.adapter.inbound.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.insureai.customer.domain.model.Nominee;

public record NomineeDto(
    @NotBlank(message = "nominee.name is required")
    @Pattern(regexp = "^\\s*\\S.{0,78}\\S\\s*$", message = "nominee.name must be between 2 and 80 characters")
    String name,

    @NotBlank(message = "nominee.relationship is required")
    @Size(max = 40, message = "nominee.relationship must not exceed 40 characters")
    String relationship
) {
    public Nominee toDomain() {
        return new Nominee(name, relationship);
    }

    public static NomineeDto fromDomain(Nominee nominee) {
        return nominee == null ? null : new NomineeDto(nominee.name(), nominee.relationship());
    }
}
