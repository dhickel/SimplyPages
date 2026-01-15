package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.Component;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Interface for components that support editing.
 *
 * <p>Replaces {@link EditAdapter} with a more flexible generic contract that supports
 * both simple property editing and nested child editing.</p>
 *
 * @param <T> The type of the editable object (for fluent chaining)
 */
public interface Editable<T> {

    /**
     * Build the edit form UI for this component.
     *
     * @return Component with form fields
     */
    Component buildEditView();

    /**
     * Apply form data to this component.
     *
     * @param formData Form field name → value map
     * @return this (for method chaining)
     */
    T applyEdits(Map<String, String> formData);

    /**
     * Validate form data before applying.
     *
     * @param formData Form field name → value map
     * @return Validation result
     */
    default ValidationResult validate(Map<String, String> formData) {
        return ValidationResult.valid();
    }

    /**
     * Get a list of editable children contained in this component.
     *
     * <p>Used by {@link EditModalBuilder} to render a list of children
     * that can be individually edited or deleted.</p>
     *
     * @return List of editable child info, or empty list if none
     */
    default List<EditableChild> getEditableChildren() {
        return Collections.emptyList();
    }
}
