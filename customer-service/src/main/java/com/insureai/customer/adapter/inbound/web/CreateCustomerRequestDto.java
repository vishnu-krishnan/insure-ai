package com.insureai.customer.adapter.inbound.web;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.insureai.customer.application.port.inbound.CreateCustomerCommand;

public record CreateCustomerRequestDto(
    @NotBlank(message = "fullName is required")
    @Size(min = 2, max = 80, message = "fullName must be between 2 and 80 characters")
    String fullName,

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Size(max = 254, message = "email must not exceed 254 characters")
    String email,

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^\\+[1-9][0-9]{7,14}$", message = "phone must be in E.164 format")
    String phone,

    @NotNull(message = "dateOfBirth is required")
    @Past(message = "dateOfBirth must be in the past")
    LocalDate dateOfBirth
) {
    public CreateCustomerCommand toCommand() {
        return new CreateCustomerCommand(fullName.trim(), email.trim(), phone.trim(), dateOfBirth);
    }
}
