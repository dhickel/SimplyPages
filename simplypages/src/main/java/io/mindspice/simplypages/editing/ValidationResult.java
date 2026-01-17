package io.mindspice.simplypages.editing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Result of form validation for editable modules.
 *
 * <p>This class encapsulates the outcome of validating form data,
 * including whether the validation passed and any error messages
 * if it failed.</p>
 *
 * <h3>Usage</h3>
 * <pre>
 * // Validation success
 * return ValidationResult.valid();
 *
 * // Validation failure with single error
 * return ValidationResult.invalid("Title cannot be empty");
 *
 * // Validation failure with multiple errors
 * return ValidationResult.invalid(
 *     "Title cannot be empty",
 *     "Content must be at least 10 characters"
 * );
 * </pre>
 */
public class ValidationResult {

    private final boolean valid;
    private final List<String> errors;

    /**
     * Private constructor. Use static factory methods.
     *
     * @param valid Whether validation passed
     * @param errors List of error messages (empty if valid)
     */
    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : Collections.emptyList();
    }

    /**
     * Create a successful validation result.
     *
     * @return ValidationResult indicating validation passed
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList());
    }

    /**
     * Create a failed validation result with error messages.
     *
     * @param errors One or more error messages
     * @return ValidationResult indicating validation failed
     */
    public static ValidationResult invalid(String... errors) {
        if (errors == null || errors.length == 0) {
            throw new IllegalArgumentException("At least one error message is required for invalid result");
        }
        return new ValidationResult(false, Arrays.asList(errors));
    }

    /**
     * Create a failed validation result with error messages.
     *
     * @param errors List of error messages
     * @return ValidationResult indicating validation failed
     */
    public static ValidationResult invalid(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("At least one error message is required for invalid result");
        }
        return new ValidationResult(false, errors);
    }

    /**
     * Check if validation passed.
     *
     * @return true if validation passed, false otherwise
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Get validation error messages.
     *
     * @return List of error messages (empty if validation passed)
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Get all error messages as a single string.
     *
     * @param separator Separator between messages (e.g., ", " or "; ")
     * @return Concatenated error messages
     */
    public String getErrorsAsString(String separator) {
        return String.join(separator, errors);
    }

    /**
     * Get all error messages as a single string (comma-separated).
     *
     * @return Concatenated error messages with comma separator
     */
    public String getErrorsAsString() {
        return getErrorsAsString(", ");
    }

    @Override
    public String toString() {
        if (valid) {
            return "ValidationResult{valid=true}";
        } else {
            return "ValidationResult{valid=false, errors=" + errors + "}";
        }
    }
}
