package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.Component;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Interface for components that support inline editing.
 * Replaces EditAdapter.
 *
 * @param <T> The type of the editable component (for fluent API)
 */
public interface Editable<T extends Editable<T>> {

    /**
     * Builds the edit form view for this component.
     * @return A form component containing input fields.
     */
    Component buildEditView();

    /**
     * Validates the form data.
     * @param formData The raw form data map.
     * @return ValidationResult indicating success or failure.
     */
    ValidationResult validate(Map<String, String> formData);

    /**
     * Applies edits to the component state.
     * @param formData The raw form data map.
     * @return This component instance.
     */
    T applyEdits(Map<String, String> formData);

    /**
     * Returns a list of editable children, if any.
     * @return List of EditableChild wrappers.
     */
    default List<EditableChild> getEditableChildren() {
        return Collections.emptyList();
    }
}
