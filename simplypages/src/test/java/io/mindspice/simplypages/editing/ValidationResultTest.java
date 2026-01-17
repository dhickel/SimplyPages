package io.mindspice.simplypages.editing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidationResultTest {

    @Test
    @DisplayName("ValidationResult.valid should be valid")
    void testValid() {
        ValidationResult result = ValidationResult.valid();
        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("ValidationResult.invalid should be invalid")
    void testInvalid() {
        ValidationResult result = ValidationResult.invalid("Error");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Error"));
    }

    @Test
    @DisplayName("ValidationResult.invalid(List) should be invalid")
    void testInvalidList() {
        ValidationResult result = ValidationResult.invalid(List.of("A", "B"));
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("A"));
    }

    @Test
    @DisplayName("ValidationResult.invalid should reject empty errors")
    void testInvalidRequiresErrors() {
        assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid());
        assertThrows(IllegalArgumentException.class, () -> ValidationResult.invalid(List.of()));
    }

    @Test
    @DisplayName("ValidationResult should format errors as strings")
    void testErrorsAsString() {
        ValidationResult result = ValidationResult.invalid("A", "B");

        assertTrue(result.getErrorsAsString("; ").equals("A; B"));
        assertTrue(result.toString().contains("errors"));
    }
}
