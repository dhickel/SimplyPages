package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.Module;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Interface for modules that support editing, including nested child components.
 * <p>
 * This replaces the primary editing contract.
 * </p>
 *
 * @param <T> The module type
 */
public interface Editable<T extends Module> {

    /**
     * Build the edit form UI for the module's main properties.
     *
     * @return Component with form fields
     */
    Component buildEditView();

    /**
     * Apply form data to this module (mutates in place).
     *
     * @param formData Form field name → value map
     * @return this (for method chaining)
     */
    T applyEdits(Map<String, String> formData);

    /**
     * Validate form data before applying.
     *
     * @param formData Form field name → value map
     * @return Validation result indicating success or failure with errors
     */
    default ValidationResult validate(Map<String, String> formData) {
        return ValidationResult.valid();
    }

    /**
     * Get a list of editable child components.
     *
     * @return List of editable children
     */
    default List<EditableChild> getEditableChildren() {
        return Collections.emptyList();
    }
}
