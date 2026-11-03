package com.insureai.customer.domain.model;

import java.util.regex.Pattern;

/** Government ID types accepted on a customer profile, each with the shape its number must have. */
public enum IdType {
    AADHAAR("^[2-9][0-9]{11}$", "Aadhaar number must be 12 digits"),
    PAN("^[A-Z]{5}[0-9]{4}[A-Z]$", "PAN must look like ABCDE1234F"),
    PASSPORT("^[A-Z][0-9]{7}$", "Passport number must be a letter followed by 7 digits"),
    DRIVING_LICENCE("^[A-Z]{2}[0-9]{2}[0-9A-Z]{11}$", "Driving licence number must be 15 letters and digits"),
    VOTER_ID("^[A-Z]{3}[0-9]{7}$", "Voter ID must be 3 letters followed by 7 digits");

    private final Pattern pattern;
    private final String message;

    IdType(String regex, String message) {
        this.pattern = Pattern.compile(regex);
        this.message = message;
    }

    public boolean accepts(String normalisedNumber) {
        return pattern.matcher(normalisedNumber).matches();
    }

    public String message() {
        return message;
    }
}
