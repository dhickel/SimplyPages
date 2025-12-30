package io.mindspice.simplypages.editing;


import io.mindspice.simplypages.core.Component;

import java.util.Map;


/**
 * Interface for handling module edit/update/delete operations.
 * <p>
 * Implement this interface in your Spring service layer to provide
 * custom editing logic and approval workflows for your modules.
 * </p>
 * <p>
 * Example implementation:
 * <pre>{@code
 * @Service
 * public class ContentModuleEditHandler implements ModuleEditHandler<ContentModule> {
 *
 *     @Override
 *     public Component renderEditForm(String moduleId) {
 *         ModuleData data = moduleRepo.findById(moduleId);
 *         return FormModule.create()
 *             .addField("Title", TextInput.create("title").withValue(data.getTitle()))
 *             .addField("Content", TextArea.create("content").withValue(data.getContent()))
 *             .addField("", Button.submit("Save")
 *                 .withAttribute("hx-post", "/api/modules/" + moduleId + "/update")
 *                 .withAttribute("hx-target", "#" + moduleId)
 *                 .withAttribute("hx-swap", "outerHTML"));
 *     }
 *
 *     @Override
 *     public Component handleUpdate(String moduleId, Map<String, String> editData, EditMode editMode) {
 *         if (editMode == EditMode.OWNER_EDIT) {
 *             moduleService.update(moduleId, editData);
 *             return EditableModule.wrap(moduleService.load(moduleId))
 *                 .withModuleId(moduleId)
 *                 .withEditUrl("/api/modules/" + moduleId + "/edit")
 *                 .withEditMode(editMode);
 *         } else {
 *             approvalService.submitEdit(moduleId, editData);
 *             return Alert.create().warning()
 *                 .withTitle("Pending Approval")
 *                 .withMessage("Your changes have been submitted for review.");
 *         }
 *     }
 *
 *     @Override
 *     public Component handleDelete(String moduleId, EditMode editMode) {
 *         if (editMode == EditMode.OWNER_EDIT) {
 *             moduleService.delete(moduleId);
 *             return new Div();  // Empty component for HTMX to remove
 *         } else {
 *             approvalService.submitDeletion(moduleId);
 *             return Alert.create().info().withMessage("Deletion request submitted.");
 *         }
 *     }
 * }
 * }</pre>
 * </p>
 *
 * @param <T> the type of module this handler manages
 */
public interface ModuleEditHandler<T> {

    /**
     * Called when a user requests to edit a module.
     * Returns a component (typically a form) for editing the module.
     * <p>
     * The returned component should include HTMX attributes to submit
     * the edit back to the update endpoint.
     * </p>
     *
     * @param moduleId the unique identifier of the module to edit
     * @return Component representing the edit form
     */
    Component renderEditForm(String moduleId);

    /**
     * Called when a user submits edits to a module.
     * <p>
     * Behavior depends on the edit mode:
     * <ul>
     *     <li>USER_EDIT: Save changes to approval queue, return approval pending message</li>
     *     <li>OWNER_EDIT: Save changes directly, return updated module</li>
     * </ul>
     * </p>
     *
     * @param moduleId the unique identifier of the module being updated
     * @param editData the submitted edit data (form field name → value)
     * @param editMode the edit mode (USER_EDIT or OWNER_EDIT)
     * @return Updated module (OWNER_EDIT) or approval message (USER_EDIT)
     */
    Component handleUpdate(String moduleId, Map<String, String> editData, EditMode editMode);

    /**
     * Called when a user requests to delete a module.
     * <p>
     * Behavior depends on the edit mode:
     * <ul>
     *     <li>USER_EDIT: Submit deletion request for approval, return confirmation message</li>
     *     <li>OWNER_EDIT: Delete immediately, return empty component</li>
     * </ul>
     * </p>
     *
     * @param moduleId the unique identifier of the module to delete
     * @param editMode the edit mode (USER_EDIT or OWNER_EDIT)
     * @return Empty component (OWNER_EDIT) or confirmation message (USER_EDIT)
     */
    Component handleDelete(String moduleId, EditMode editMode);
}
