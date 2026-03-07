package io.mindspice.simplypages.editing;


import io.mindspice.simplypages.core.Component;

import java.util.Map;


/**
 * Application service contract for edit form rendering, update handling, and delete handling.
 *
 * <p>Framework boundary: SimplyPages invokes these hooks but does not decide persistence model,
 * approval workflow, or authorization policy.</p>
 *
 * @param <T> module type handled by this service
 */
public interface ModuleEditHandler<T> {

    /**
     * Renders edit UI for a module id.
     *
     * @param moduleId the unique identifier of the module to edit
     * @return Component representing the edit form
     */
    Component renderEditForm(String moduleId);

    /**
     * Handles submitted edit payload.
     *
     * <p>Implementations typically branch on {@code editMode} to either apply immediately or
     * enqueue approval. Returned component is rendered back to caller/HTMX target.</p>
     *
     * @param moduleId the unique identifier of the module being updated
     * @param editData the submitted edit data (form field name → value)
     * @param editMode the edit mode (USER_EDIT or OWNER_EDIT)
     * @return Updated module (OWNER_EDIT) or approval message (USER_EDIT)
     */
    Component handleUpdate(String moduleId, Map<String, String> editData, EditMode editMode);

    /**
     * Handles delete request for a module id.
     *
     * <p>Implementations may apply immediate deletion or return approval-state feedback based on
     * {@code editMode}.</p>
     *
     * @param moduleId the unique identifier of the module to delete
     * @param editMode the edit mode (USER_EDIT or OWNER_EDIT)
     * @return Empty component (OWNER_EDIT) or confirmation message (USER_EDIT)
     */
    Component handleDelete(String moduleId, EditMode editMode);
}
