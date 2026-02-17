package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.Module;

/**
 * Wrapper that adds edit/delete buttons to any module.
 *
 * <p>EditableModule is a <strong>decorator</strong> that wraps existing modules and provides
 * framework-level edit and delete buttons with proper styling, z-index, and positioning.</p>
 *
 * <h2>Why EditableModule?</h2>
 * <ul>
 *   <li><strong>Framework-Level Styling:</strong> Edit/delete buttons styled via framework CSS classes</li>
 *   <li><strong>Z-Index Management:</strong> Buttons automatically layer above module content</li>
 *   <li><strong>Consistent UX:</strong> Standard button appearance and positioning across all modules</li>
 *   <li><strong>No Manual Styling:</strong> End users don't need to add CSS for edit buttons</li>
 * </ul>
 *
 * <h2>Usage</h2>
 *
 * <h3>Basic Edit Functionality</h3>
 * <pre>{@code
 * ContentModule content = ContentModule.create()
 *     .withTitle("My Content")
 *     .withContent("# Welcome");
 *
 * EditableModule editable = EditableModule.wrap(content)
 *     .withEditUrl("/edit/" + moduleId)
 *     .withDeleteUrl("/delete/" + moduleId);
 *
 * // Renders module with ✏ edit and 🗑 delete buttons in top-right
 * String html = editable.render();
 * }</pre>
 *
 * <h3>Edit-Only (No Delete)</h3>
 * <pre>{@code
 * EditableModule editable = EditableModule.wrap(module)
 *     .withEditUrl("/edit/" + moduleId);
 * // Only shows ✏ edit button
 * }</pre>
 *
 * <h3>Delete-Only (No Edit)</h3>
 * <pre>{@code
 * EditableModule editable = EditableModule.wrap(module)
 *     .withDeleteUrl("/delete/" + moduleId);
 * // Only shows 🗑 delete button
 * }</pre>
 *
 * <h3>With Confirmation Dialog</h3>
 * <pre>{@code
 * EditableModule editable = EditableModule.wrap(module)
 *     .withEditUrl("/edit/123")
 *     .withDeleteUrl("/delete/123")
 *     .withDeleteConfirm("Are you sure you want to delete this module?");
 * }</pre>
 *
 * <h3>Custom Button Labels</h3>
 * <pre>{@code
 * EditableModule editable = EditableModule.wrap(module)
 *     .withEditUrl("/edit/123")
 *     .withEditButton("✎")  // Custom edit icon
 *     .withDeleteUrl("/delete/123")
 *     .withDeleteButton("✖");  // Custom delete icon
 * }</pre>
 *
 * <h2>HTMX Integration</h2>
 *
 * <p>EditableModule automatically sets up HTMX attributes for edit and delete operations:</p>
 * <ul>
 *   <li><strong>Edit:</strong> Uses {@code hx-get} to fetch edit modal</li>
 *   <li><strong>Delete:</strong> Uses {@code hx-delete} to remove module</li>
 *   <li><strong>Target:</strong> Edit targets {@code #edit-modal-container} by default</li>
 *   <li><strong>Swap:</strong> Edit uses {@code innerHTML} swap by default</li>
 * </ul>
 *
 * <h3>Custom HTMX Configuration</h3>
 * <pre>{@code
 * EditableModule editable = EditableModule.wrap(module)
 *     .withEditUrl("/edit/123")
 *     .withEditTarget("#custom-modal")  // Custom target
 *     .withDeleteUrl("/delete/123")
 *     .withDeleteTarget("#page-content")  // Custom delete target
 *     .withDeleteSwap("outerHTML");  // Custom swap strategy
 * }</pre>
 *
 * <h2>Framework CSS Classes</h2>
 *
 * <p>EditableModule uses framework CSS classes for consistent styling:</p>
 * <ul>
 *   <li><strong>.editable-module-wrapper:</strong> Wrapper div with {@code position: relative}</li>
 *   <li><strong>.module-edit-btn:</strong> Edit button style (gray, top-right: 30px, z-index: 10)</li>
 *   <li><strong>.module-delete-btn:</strong> Delete button style (red, top-right: 2px, z-index: 10)</li>
 * </ul>
 *
 * <p><strong>Note:</strong> Users don't need to add any custom CSS. All styling is framework-level.</p>
 *
 * <h2>Design Principles</h2>
 * <ul>
 *   <li><strong>Decorator Pattern:</strong> Wraps existing modules without modifying them</li>
 *   <li><strong>Framework-Level:</strong> All styling and positioning handled by framework</li>
 *   <li><strong>Single Modal Container:</strong> Edit button targets {@code #edit-modal-container}</li>
 *   <li><strong>Z-Index Automatic:</strong> Framework CSS ensures buttons appear above content</li>
 *   <li><strong>Fluent API:</strong> All configuration methods return {@code this}</li>
 * </ul>
 *
 * @see io.mindspice.simplypages.editing.Editable
 * @see io.mindspice.simplypages.editing.EditModalBuilder
 * @see io.mindspice.simplypages.components.display.Modal
 */
public class EditableModule extends Div {

    private static int idCounter = 0;  // For generating unique IDs

    private final Component wrappedModule;
    private boolean built = false;  // Track if buildWrapper has been called
    private String moduleId;  // ID for targeting

    // Permission flags (Phase 6.5)
    private boolean canEdit = true;
    private boolean canDelete = true;
    private io.mindspice.simplypages.editing.EditMode editMode;

    // Edit configuration
    private String editUrl;
    private String editButtonLabel = "✏";
    private String editTarget = "#edit-modal-container";
    private String editSwap = "innerHTML";
    private String editTitle = "Edit";

    // Delete configuration
    private String deleteUrl;
    private String deleteButtonLabel = "🗑";
    private String deleteTarget;  // Default is to swap the wrapper itself
    private String deleteSwap = "outerHTML";
    private String deleteConfirm;
    private String deleteTitle = "Delete";

    /**
     * Creates a new EditableModule wrapping the specified module.
     *
     * @param wrappedModule the module to wrap with edit/delete functionality
     */
    private EditableModule(Component wrappedModule) {
        super();
        this.wrappedModule = wrappedModule;
        this.withClass("editable-module-wrapper");
        // Generate a unique ID for targeting
        this.moduleId = "editable-module-" + (++idCounter);
        this.withAttribute("id", this.moduleId);
    }

    /**
     * Wraps a module with edit/delete functionality.
     *
     * @param module the module to wrap
     * @return new EditableModule instance
     */
    public static EditableModule wrap(Component module) {
        return new EditableModule(module);
    }

    /**
     * Sets a custom module ID for this wrapper.
     * This ID is used for HTMX targeting and as the DOM id attribute.
     *
     * @param id the module ID
     * @return this EditableModule for method chaining
     */
    public EditableModule withModuleId(String id) {
        this.moduleId = id;
        this.withAttribute("id", id);
        return this;
    }

    /**
     * Returns the module ID used for targeting.
     *
     * @return the module ID
     */
    public String getModuleId() {
        return moduleId;
    }

    // ===== Permission Configuration (Phase 6.5) =====

    /**
     * Sets whether this module can be edited.
     * If false, the edit button will not be displayed even if editUrl is set.
     *
     * @param canEdit true to allow editing, false to lock editing
     * @return this EditableModule for method chaining
     */
    public EditableModule withCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
        return this;
    }

    /**
     * Sets whether this module can be deleted.
     * If false, the delete button will not be displayed even if deleteUrl is set.
     *
     * @param canDelete true to allow deletion, false to lock deletion
     * @return this EditableModule for method chaining
     */
    public EditableModule withCanDelete(boolean canDelete) {
        this.canDelete = canDelete;
        return this;
    }

    /**
     * Sets the edit mode for this module.
     * <p>
     * The edit mode is appended as a query parameter to edit/delete URLs
     * so the server can apply appropriate approval workflows.
     * </p>
     *
     * @param editMode the edit mode (USER_EDIT requires approval, OWNER_EDIT is immediate)
     * @return this EditableModule for method chaining
     */
    public EditableModule withEditMode(io.mindspice.simplypages.editing.EditMode editMode) {
        this.editMode = editMode;
        return this;
    }

    // ===== Edit Configuration =====

    /**
     * Sets the URL for editing this module (HTMX hx-get).
     *
     * @param editUrl the endpoint to fetch edit modal from
     * @return this EditableModule for method chaining
     */
    public EditableModule withEditUrl(String editUrl) {
        this.editUrl = editUrl;
        return this;
    }

    /**
     * Sets custom label for the edit button (default: "✏").
     *
     * @param label the button label (emoji or text)
     * @return this EditableModule for method chaining
     */
    public EditableModule withEditButton(String label) {
        this.editButtonLabel = label;
        return this;
    }

    /**
     * Sets the HTMX target for edit operations (default: "#edit-modal-container").
     *
     * @param target the CSS selector for the target element
     * @return this EditableModule for method chaining
     */
    public EditableModule withEditTarget(String target) {
        this.editTarget = target;
        return this;
    }

    /**
     * Sets the HTMX swap strategy for edit operations (default: "innerHTML").
     *
     * @param swap the swap strategy
     * @return this EditableModule for method chaining
     */
    public EditableModule withEditSwap(String swap) {
        this.editSwap = swap;
        return this;
    }

    /**
     * Sets the title attribute for the edit button (default: "Edit").
     *
     * @param title the tooltip text
     * @return this EditableModule for method chaining
     */
    public EditableModule withEditTitle(String title) {
        this.editTitle = title;
        return this;
    }

    // ===== Delete Configuration =====

    /**
     * Sets the URL for deleting this module (HTMX hx-delete).
     *
     * @param deleteUrl the endpoint to delete the module
     * @return this EditableModule for method chaining
     */
    public EditableModule withDeleteUrl(String deleteUrl) {
        this.deleteUrl = deleteUrl;
        return this;
    }

    /**
     * Sets custom label for the delete button (default: "🗑").
     *
     * @param label the button label (emoji or text)
     * @return this EditableModule for method chaining
     */
    public EditableModule withDeleteButton(String label) {
        this.deleteButtonLabel = label;
        return this;
    }

    /**
     * Sets the HTMX target for delete operations.
     * If not set, the module itself will be swapped out.
     *
     * @param target the CSS selector for the target element
     * @return this EditableModule for method chaining
     */
    public EditableModule withDeleteTarget(String target) {
        this.deleteTarget = target;
        return this;
    }

    /**
     * Sets the HTMX swap strategy for delete operations (default: "outerHTML").
     *
     * @param swap the swap strategy
     * @return this EditableModule for method chaining
     */
    public EditableModule withDeleteSwap(String swap) {
        this.deleteSwap = swap;
        return this;
    }

    /**
     * Sets a confirmation message for delete operations.
     *
     * @param message the confirmation prompt text
     * @return this EditableModule for method chaining
     */
    public EditableModule withDeleteConfirm(String message) {
        this.deleteConfirm = message;
        return this;
    }

    /**
     * Sets the title attribute for the delete button (default: "Delete").
     *
     * @param title the tooltip text
     * @return this EditableModule for method chaining
     */
    public EditableModule withDeleteTitle(String title) {
        this.deleteTitle = title;
        return this;
    }

    /**
     * Appends editMode query parameter to URL if editMode is set.
     */
    private String appendEditModeParam(String url) {
        if (editMode == null || url == null) {
            return url;
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "editMode=" + editMode.name();
    }

    /**
     * Builds the wrapped module with edit/delete buttons.
     *
     * <p>This method is called automatically during render.</p>
     * <p>This method is idempotent - it only builds once.</p>
     */
    private void buildWrapper() {
        if (built) {
            return;  // Already built, don't build again
        }
        built = true;

        // Add edit button if URL is set AND editing is permitted
        if (canEdit && editUrl != null && !editUrl.isEmpty()) {
            Button editBtn = Button.create(editButtonLabel)
                    .withStyle(Button.ButtonStyle.LINK)
                    .withClass("module-edit-btn");

            editBtn.withAttribute("hx-get", appendEditModeParam(editUrl));
            editBtn.withAttribute("hx-target", editTarget);
            editBtn.withAttribute("hx-swap", editSwap);
            editBtn.withAttribute("title", editTitle);

            super.withChild(editBtn);
        }

        // Add delete button if URL is set AND deletion is permitted
        if (canDelete && deleteUrl != null && !deleteUrl.isEmpty()) {
            Button deleteBtn = Button.create(deleteButtonLabel)
                    .withStyle(Button.ButtonStyle.LINK)
                    .withClass("module-delete-btn");

            deleteBtn.withAttribute("hx-delete", appendEditModeParam(deleteUrl));

            // Set target - default to the wrapper's ID if not explicitly specified
            String target = (deleteTarget != null) ? deleteTarget : "#" + moduleId;
            deleteBtn.withAttribute("hx-target", target);
            deleteBtn.withAttribute("hx-swap", deleteSwap);

            if (deleteConfirm != null && !deleteConfirm.isEmpty()) {
                deleteBtn.withAttribute("hx-confirm", deleteConfirm);
            }
            deleteBtn.withAttribute("title", deleteTitle);

            super.withChild(deleteBtn);
        }

        // Add the wrapped module
        super.withChild(wrappedModule);
    }

    /**
     * Renders this EditableModule and its wrapped module with context.
     *
     * @param context the render context
     * @return HTML string with wrapper, buttons, and module
     */
    @Override
    public String render(io.mindspice.simplypages.core.RenderContext context) {
        buildWrapper();
        return super.render(context);
    }
}
