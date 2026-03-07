package io.mindspice.simplypages.editing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Immutable validation outcome for edit-form processing.
 *
 * <p>Contract: success is represented by {@link #isValid()} and optional error messages are
 * exposed in insertion order.</p>
 *
 * <p>Mutability and thread-safety: immutable and thread-safe.</p>
 */
public class ValidationResult {

    private final boolean valid;
    private final List<String> errors;

    /**
     * Internal constructor used by factory methods.
     *
     * @param valid Whether validation passed
     * @param errors List of error messages (empty if valid)
     */
    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors != null ? errors : Collections.emptyList();
    }

    /** Creates a successful validation result. */
    public static ValidationResult valid() {
        return new ValidationResult(true, Collections.emptyList());
    }

    /**
     * Creates a failed validation result from one or more error messages.
     */
    public static ValidationResult invalid(String... errors) {
        if (errors == null || errors.length == 0) {
            throw new IllegalArgumentException("At least one error message is required for invalid result");
        }
        return new ValidationResult(false, Arrays.asList(errors));
    }

    /**
     * Creates a failed validation result from a message list.
     */
    public static ValidationResult invalid(List<String> errors) {
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException("At least one error message is required for invalid result");
        }
        return new ValidationResult(false, errors);
    }

    /** Returns whether validation succeeded. */
    public boolean isValid() {
        return valid;
    }

    /** Returns validation errors, or an empty list when valid. */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * Joins all errors with a caller-provided separator.
     */
    public String getErrorsAsString(String separator) {
        return String.join(separator, errors);
    }

    /** Joins all errors with a comma separator. */
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
