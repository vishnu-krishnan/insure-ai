package com.insureai.customer.domain.model;

public record Nominee(String name, String relationship) {
    public Nominee {
        if (name == null || relationship == null) {
            throw new IllegalArgumentException("Nominee name and relationship are required");
        }
        name = name.trim();
        relationship = relationship.trim();
    }
}
