package com.insureai.insurance.domain.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.insureai.insurance.domain.exception.DomainValidationException;

public final class QuoteTypeDetails {
    private static final String PREFIX = "details.";

    private static final List<FieldRule> AUTO_FIELDS = List.of(
        FieldRule.text("vehicleMake", 1, 60),
        FieldRule.text("vehicleModel", 1, 60),
        FieldRule.integer("vehicleYear", 1980, Bound.NEXT_YEAR),
        FieldRule.text("registrationNumber", 4, 20),
        FieldRule.choice("usage", "PRIVATE", "COMMERCIAL"),
        FieldRule.integer("previousClaims", 0, 20));
    private static final List<FieldRule> HOME_FIELDS = List.of(
        FieldRule.choice("propertyType", "APARTMENT", "HOUSE", "VILLA"),
        FieldRule.integer("builtYear", 1900, Bound.CURRENT_YEAR),
        FieldRule.integer("areaSqFt", 50, 100000),
        FieldRule.choice("ownership", "OWNED", "RENTED"),
        FieldRule.text("city", 1, 60));
    private static final List<FieldRule> LIFE_FIELDS = List.of(
        FieldRule.flag("smoker"),
        FieldRule.choice("occupationClass", "CLASS_1", "CLASS_2", "CLASS_3"),
        FieldRule.text("nomineeName", 1, 80),
        FieldRule.text("nomineeRelationship", 1, 40));
    private static final List<FieldRule> HEALTH_FIELDS = List.of(
        FieldRule.integer("members", 1, 10),
        FieldRule.flag("preExistingConditions"),
        FieldRule.flag("smoker"),
        FieldRule.choice("planTier", "BASIC", "STANDARD", "PREMIUM"));

    private QuoteTypeDetails() {
    }

    public static Map<String, Object> validate(CoverageType coverageType, Map<String, Object> details, int currentYear) {
        if (details == null) {
            return null;
        }
        List<FieldRule> rules = rulesFor(coverageType);

        for (String key : details.keySet()) {
            if (rules.stream().noneMatch(rule -> rule.name.equals(key))) {
                throw new DomainValidationException(
                    PREFIX + key + " is not a valid field for " + coverageType.getCode() + " quotes", PREFIX + key);
            }
        }

        Map<String, Object> validated = new LinkedHashMap<>();
        for (FieldRule rule : rules) {
            validated.put(rule.name, rule.validate(details.get(rule.name), currentYear));
        }
        return validated;
    }

    private static List<FieldRule> rulesFor(CoverageType coverageType) {
        return switch (coverageType) {
            case AUTO -> AUTO_FIELDS;
            case HOME -> HOME_FIELDS;
            case LIFE -> LIFE_FIELDS;
            case HEALTH -> HEALTH_FIELDS;
        };
    }

    private enum Bound { FIXED, CURRENT_YEAR, NEXT_YEAR }

    private enum Kind { TEXT, INTEGER, FLAG, CHOICE }

    private static final class FieldRule {
        private final String name;
        private final Kind kind;
        private final int min;
        private final int max;
        private final Bound maxBound;
        private final List<String> choices;

        private FieldRule(String name, Kind kind, int min, int max, Bound maxBound, List<String> choices) {
            this.name = name;
            this.kind = kind;
            this.min = min;
            this.max = max;
            this.maxBound = maxBound;
            this.choices = choices;
        }

        static FieldRule text(String name, int min, int max) {
            return new FieldRule(name, Kind.TEXT, min, max, Bound.FIXED, List.of());
        }

        static FieldRule integer(String name, int min, int max) {
            return new FieldRule(name, Kind.INTEGER, min, max, Bound.FIXED, List.of());
        }

        static FieldRule integer(String name, int min, Bound maxBound) {
            return new FieldRule(name, Kind.INTEGER, min, 0, maxBound, List.of());
        }

        static FieldRule flag(String name) {
            return new FieldRule(name, Kind.FLAG, 0, 0, Bound.FIXED, List.of());
        }

        static FieldRule choice(String name, String... choices) {
            return new FieldRule(name, Kind.CHOICE, 0, 0, Bound.FIXED, List.of(choices));
        }

        Object validate(Object value, int currentYear) {
            String field = PREFIX + name;
            if (value == null) {
                throw new DomainValidationException(field + " is required", field);
            }
            return switch (kind) {
                case TEXT -> validateText(field, value);
                case INTEGER -> validateInteger(field, value, currentYear);
                case FLAG -> validateFlag(field, value);
                case CHOICE -> validateChoice(field, value);
            };
        }

        private Object validateText(String field, Object value) {
            if (!(value instanceof String text)) {
                throw new DomainValidationException(field + " must be a string", field);
            }
            String trimmed = text.trim();
            if (trimmed.length() < min || trimmed.length() > max) {
                throw new DomainValidationException(
                    field + " must be between " + min + " and " + max + " characters", field);
            }
            return trimmed;
        }

        private Object validateInteger(String field, Object value, int currentYear) {
            int upper = switch (maxBound) {
                case FIXED -> max;
                case CURRENT_YEAR -> currentYear;
                case NEXT_YEAR -> currentYear + 1;
            };
            boolean whole = value instanceof Integer || value instanceof Long
                || value instanceof Short || value instanceof Byte;
            if (!whole) {
                throw new DomainValidationException(field + " must be an integer", field);
            }
            long number = ((Number) value).longValue();
            if (number < min || number > upper) {
                throw new DomainValidationException(field + " must be between " + min + " and " + upper, field);
            }
            return (int) number;
        }

        private Object validateFlag(String field, Object value) {
            if (!(value instanceof Boolean)) {
                throw new DomainValidationException(field + " must be true or false", field);
            }
            return value;
        }

        private Object validateChoice(String field, Object value) {
            if (!(value instanceof String text) || !choices.contains(text)) {
                throw new DomainValidationException(
                    field + " must be one of " + String.join(", ", choices), field);
            }
            return text;
        }
    }
}
