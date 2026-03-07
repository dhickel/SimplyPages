package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.Module;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Primary editing contract for mutable SimplyPages modules.
 *
 * <p>Framework boundary: this interface defines rendering and mutation hooks only. Applications
 * are responsible for authorization, CSRF, transport validation, and persistence policy.</p>
 *
 * <p>Mutability and thread-safety: implementations are typically mutable and request-scoped.
 * Treat editable module instances as not thread-safe unless an implementation documents otherwise.</p>
 *
 * @param <T> editable module type
 */
public interface Editable<T extends Module> {

    /**
     * Builds the main-property edit view for this module.
     *
     * @return component containing form controls
     */
    Component buildEditView();

    /**
     * Applies form data to this module instance.
     *
     * <p>Side effect: mutates this module. Implementations should rebuild cached/built structure
     * when required by module lifecycle semantics.</p>
     *
     * @param formData form field name to value map
     * @return this module instance
     */
    T applyEdits(Map<String, String> formData);

    /**
     * Validates form data before mutation.
     *
     * @param formData form field name to value map
     * @return validation result; default is success
     */
    default ValidationResult validate(Map<String, String> formData) {
        return ValidationResult.valid();
    }

    /**
     * Returns editable child handles for nested editing UIs.
     *
     * @return editable children in UI/display order; default is empty
     */
    default List<EditableChild> getEditableChildren() {
        return Collections.emptyList();
    }
}
