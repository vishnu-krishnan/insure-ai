package com.insureai.customer.application.port.inbound;

import java.time.LocalDate;

public record CreateCustomerCommand(String fullName, String email, String phone, LocalDate dateOfBirth) {
}
