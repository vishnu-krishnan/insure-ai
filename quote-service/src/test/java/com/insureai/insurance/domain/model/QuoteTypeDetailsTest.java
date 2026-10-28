package com.insureai.insurance.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.insureai.insurance.domain.exception.DomainValidationException;

class QuoteTypeDetailsTest {
    private static final int YEAR = 2026;

    @Test
    void absentDetailsAreAllowedForEveryType() {
        for (CoverageType type : CoverageType.values()) {
            assertNull(QuoteTypeDetails.validate(type, null, YEAR));
        }
    }

    @Test
    void acceptsValidDetailsForEveryType() {
        assertEquals(auto(), QuoteTypeDetails.validate(CoverageType.AUTO, auto(), YEAR));
        assertEquals(home(), QuoteTypeDetails.validate(CoverageType.HOME, home(), YEAR));
        assertEquals(life(), QuoteTypeDetails.validate(CoverageType.LIFE, life(), YEAR));
        assertEquals(health(), QuoteTypeDetails.validate(CoverageType.HEALTH, health(), YEAR));
    }

    @Test
    void acceptsBoundaryValues() {
        Map<String, Object> autoEdges = auto();
        autoEdges.put("vehicleYear", 1980);
        autoEdges.put("previousClaims", 0);
        QuoteTypeDetails.validate(CoverageType.AUTO, autoEdges, YEAR);
        autoEdges.put("vehicleYear", YEAR + 1);
        autoEdges.put("previousClaims", 20);
        autoEdges.put("registrationNumber", "ABCD");
        QuoteTypeDetails.validate(CoverageType.AUTO, autoEdges, YEAR);

        Map<String, Object> homeEdges = home();
        homeEdges.put("builtYear", YEAR);
        homeEdges.put("areaSqFt", 50);
        QuoteTypeDetails.validate(CoverageType.HOME, homeEdges, YEAR);
        homeEdges.put("builtYear", 1900);
        homeEdges.put("areaSqFt", 100000);
        QuoteTypeDetails.validate(CoverageType.HOME, homeEdges, YEAR);

        Map<String, Object> healthEdges = health();
        healthEdges.put("members", 1);
        QuoteTypeDetails.validate(CoverageType.HEALTH, healthEdges, YEAR);
        healthEdges.put("members", 10);
        QuoteTypeDetails.validate(CoverageType.HEALTH, healthEdges, YEAR);
    }

    @Test
    void rejectsMissingFieldForEveryType() {
        assertRejected(CoverageType.AUTO, without(auto(), "usage"), "details.usage");
        assertRejected(CoverageType.HOME, without(home(), "city"), "details.city");
        assertRejected(CoverageType.LIFE, without(life(), "nomineeName"), "details.nomineeName");
        assertRejected(CoverageType.HEALTH, without(health(), "planTier"), "details.planTier");
    }

    @Test
    void rejectsEmptyObjectAndNullValue() {
        assertRejected(CoverageType.AUTO, new LinkedHashMap<>(), "details.vehicleMake");
        Map<String, Object> nullValue = auto();
        nullValue.put("vehicleModel", null);
        assertRejected(CoverageType.AUTO, nullValue, "details.vehicleModel");
    }

    @Test
    void rejectsWrongEnumValue() {
        assertRejected(CoverageType.AUTO, with(auto(), "usage", "RACING"), "details.usage");
        assertRejected(CoverageType.AUTO, with(auto(), "usage", "private"), "details.usage");
        assertRejected(CoverageType.HOME, with(home(), "propertyType", "CASTLE"), "details.propertyType");
        assertRejected(CoverageType.HOME, with(home(), "ownership", "LEASED"), "details.ownership");
        assertRejected(CoverageType.LIFE, with(life(), "occupationClass", "CLASS_9"), "details.occupationClass");
        assertRejected(CoverageType.HEALTH, with(health(), "planTier", "GOLD"), "details.planTier");
    }

    @Test
    void rejectsOutOfRangeValues() {
        assertRejected(CoverageType.AUTO, with(auto(), "vehicleYear", 1979), "details.vehicleYear");
        assertRejected(CoverageType.AUTO, with(auto(), "vehicleYear", YEAR + 2), "details.vehicleYear");
        assertRejected(CoverageType.AUTO, with(auto(), "previousClaims", -1), "details.previousClaims");
        assertRejected(CoverageType.AUTO, with(auto(), "previousClaims", 21), "details.previousClaims");
        assertRejected(CoverageType.AUTO, with(auto(), "registrationNumber", "ABC"), "details.registrationNumber");
        assertRejected(CoverageType.AUTO, with(auto(), "registrationNumber", "A".repeat(21)),
            "details.registrationNumber");
        assertRejected(CoverageType.AUTO, with(auto(), "vehicleMake", "M".repeat(61)), "details.vehicleMake");
        assertRejected(CoverageType.AUTO, with(auto(), "vehicleMake", "   "), "details.vehicleMake");
        assertRejected(CoverageType.HOME, with(home(), "builtYear", 1899), "details.builtYear");
        assertRejected(CoverageType.HOME, with(home(), "builtYear", YEAR + 1), "details.builtYear");
        assertRejected(CoverageType.HOME, with(home(), "areaSqFt", 49), "details.areaSqFt");
        assertRejected(CoverageType.HOME, with(home(), "areaSqFt", 100001), "details.areaSqFt");
        assertRejected(CoverageType.HOME, with(home(), "city", "C".repeat(61)), "details.city");
        assertRejected(CoverageType.LIFE, with(life(), "nomineeName", "N".repeat(81)), "details.nomineeName");
        assertRejected(CoverageType.LIFE, with(life(), "nomineeRelationship", ""), "details.nomineeRelationship");
        assertRejected(CoverageType.LIFE, with(life(), "nomineeRelationship", "R".repeat(41)),
            "details.nomineeRelationship");
        assertRejected(CoverageType.HEALTH, with(health(), "members", 0), "details.members");
        assertRejected(CoverageType.HEALTH, with(health(), "members", 11), "details.members");
    }

    @Test
    void rejectsWrongValueTypes() {
        assertRejected(CoverageType.AUTO, with(auto(), "vehicleYear", "2020"), "details.vehicleYear");
        assertRejected(CoverageType.AUTO, with(auto(), "vehicleYear", 2020.5), "details.vehicleYear");
        assertRejected(CoverageType.AUTO, with(auto(), "vehicleMake", 7), "details.vehicleMake");
        assertRejected(CoverageType.LIFE, with(life(), "smoker", "yes"), "details.smoker");
        assertRejected(CoverageType.HEALTH, with(health(), "preExistingConditions", 1), "details.preExistingConditions");
        assertRejected(CoverageType.HEALTH, with(health(), "members", Map.of()), "details.members");
    }

    @Test
    void rejectsUnknownKey() {
        assertRejected(CoverageType.AUTO, with(auto(), "colour", "red"), "details.colour");
        assertRejected(CoverageType.HOME, with(home(), "pool", true), "details.pool");
        assertRejected(CoverageType.LIFE, with(life(), "bmi", 22), "details.bmi");
        assertRejected(CoverageType.HEALTH, with(health(), "smokerYears", 3), "details.smokerYears");
    }

    @Test
    void rejectsDetailsForTheWrongType() {
        DomainValidationException wrong = assertThrows(DomainValidationException.class,
            () -> QuoteTypeDetails.validate(CoverageType.HOME, auto(), YEAR));
        assertEquals("details.vehicleMake", wrong.field());
        assertRejected(CoverageType.AUTO, health(), "details.members");
        assertRejected(CoverageType.LIFE, home(), "details.propertyType");
        assertRejected(CoverageType.HEALTH, life(), "details.occupationClass");
    }

    @Test
    void trimsStringsAndKeepsNumberAndBooleanTypes() {
        Map<String, Object> padded = auto();
        padded.put("vehicleMake", "  Toyota ");
        Map<String, Object> result = QuoteTypeDetails.validate(CoverageType.AUTO, padded, YEAR);

        assertEquals("Toyota", result.get("vehicleMake"));
        assertEquals(2020, result.get("vehicleYear"));
        Map<String, Object> life = QuoteTypeDetails.validate(CoverageType.LIFE, life(), YEAR);
        assertEquals(Boolean.FALSE, life.get("smoker"));
    }

    private static void assertRejected(CoverageType type, Map<String, Object> details, String field) {
        DomainValidationException exception = assertThrows(DomainValidationException.class,
            () -> QuoteTypeDetails.validate(type, details, YEAR), field);
        assertEquals(field, exception.field());
    }

    private static Map<String, Object> with(Map<String, Object> map, String key, Object value) {
        map.put(key, value);
        return map;
    }

    private static Map<String, Object> without(Map<String, Object> map, String key) {
        map.remove(key);
        return map;
    }

    static Map<String, Object> auto() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("vehicleMake", "Toyota");
        map.put("vehicleModel", "Corolla");
        map.put("vehicleYear", 2020);
        map.put("registrationNumber", "KA01AB1234");
        map.put("usage", "PRIVATE");
        map.put("previousClaims", 1);
        return map;
    }

    static Map<String, Object> home() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("propertyType", "APARTMENT");
        map.put("builtYear", 2010);
        map.put("areaSqFt", 1200);
        map.put("ownership", "OWNED");
        map.put("city", "Bengaluru");
        return map;
    }

    static Map<String, Object> life() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("smoker", false);
        map.put("occupationClass", "CLASS_1");
        map.put("nomineeName", "Jane Doe");
        map.put("nomineeRelationship", "Spouse");
        return map;
    }

    static Map<String, Object> health() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("members", 4);
        map.put("preExistingConditions", false);
        map.put("smoker", true);
        map.put("planTier", "STANDARD");
        return map;
    }
}
