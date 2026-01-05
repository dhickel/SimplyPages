package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import java.util.regex.Pattern;

/**
 * Helper for building standardized edit modals.
 *
 * <p>This builder simplifies the creation of edit modals with a consistent
 * layout and behavior. It handles:</p>
 * <ul>
 *   <li>Entity property editing (via {@link EditAdapter#buildEditView()})</li>
 *   <li>Footer layout (delete on left, cancel/save on right)</li>
 *   <li>HTMX attributes for dynamic updates</li>
 * </ul>
 *
 * <h3>Usage Example</h3>
 * <pre>
 * {@literal @}GetMapping("/api/modules/{id}/edit")
 * public String editModule(@PathVariable String id) {
 *     Module module = findModule(id);
 *     EditAdapter adapter = (EditAdapter) module;
 *
 *     return EditModalBuilder.create()
 *         .withTitle("Edit Content Module")
 *         .withModuleId(id)
 *         .withEditView(adapter.buildEditView())
 *         .withSaveUrl("/api/modules/" + id + "/update")
 *         .withDeleteUrl("/api/modules/" + id + "/delete")
 *         .build()
 *         .render();
 * }
 * </pre>
 */
public class EditModalBuilder {

    private static final Pattern VALID_ID_PATTERN =
        Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");

    private String title = "Edit Module";
    private String moduleId;
    private Component editView;
    private String saveUrl;
    private String deleteUrl;
    private boolean showDelete = true;
    private String pageContainerId = "page-content";
    private String modalContainerId = "edit-modal-container";

    /**
     * Private constructor. Use create() factory method.
     */
    private EditModalBuilder() {
    }

    /**
     * Create a new EditModalBuilder instance.
     *
     * @return A new builder instance
     */
    public static EditModalBuilder create() {
        return new EditModalBuilder();
    }

    /**
     * Set the modal title.
     *
     * @param title The modal title
     * @return this for chaining
     */
    public EditModalBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    /**
     * Set the module ID being edited.
     *
     * @param moduleId The module identifier
     * @return this for chaining
     */
    public EditModalBuilder withModuleId(String moduleId) {
        this.moduleId = moduleId;
        return this;
    }

    /**
     * Set the edit view component (form fields) directly.
     *
     * @param editView The edit form component
     * @return this for chaining
     */
    public EditModalBuilder withEditView(Component editView) {
        this.editView = editView;
        return this;
    }

    /**
     * Set the save endpoint URL.
     *
     * <p>The save button will POST to this URL with form data.</p>
     *
     * @param saveUrl The save endpoint URL
     * @return this for chaining
     */
    public EditModalBuilder withSaveUrl(String saveUrl) {
        this.saveUrl = saveUrl;
        return this;
    }

    /**
     * Set the delete endpoint URL.
     *
     * <p>The delete button will DELETE to this URL.</p>
     *
     * @param deleteUrl The delete endpoint URL
     * @return this for chaining
     */
    public EditModalBuilder withDeleteUrl(String deleteUrl) {
        this.deleteUrl = deleteUrl;
        return this;
    }

    /**
     * Hide the delete button.
     *
     * <p>Use this when adding new modules or when deletion is not allowed.</p>
     *
     * @return this for chaining
     */
    public EditModalBuilder hideDelete() {
        this.showDelete = false;
        return this;
    }

    /**
     * Set the page container ID for HTMX target.
     *
     * <p>Default is "page-content".</p>
     *
     * @param pageContainerId The page container element ID
     * @return this for chaining
     */
    public EditModalBuilder withPageContainerId(String pageContainerId) {
        if (pageContainerId == null || !VALID_ID_PATTERN.matcher(pageContainerId).matches()) {
            throw new IllegalArgumentException(
                "Page container ID must start with a letter and contain only letters, numbers, hyphens, and underscores. Got: "
                    + pageContainerId);
        }
        this.pageContainerId = pageContainerId;
        return this;
    }

    /**
     * Set the modal container ID for HTMX target.
     *
     * <p>Default is "edit-modal-container".</p>
     *
     * @param modalContainerId The modal container element ID
     * @return this for chaining
     */
    public EditModalBuilder withModalContainerId(String modalContainerId) {
        if (modalContainerId == null || !VALID_ID_PATTERN.matcher(modalContainerId).matches()) {
            throw new IllegalArgumentException(
                "Modal container ID must start with a letter and contain only letters, numbers, hyphens, and underscores. Got: "
                    + modalContainerId);
        }
        this.modalContainerId = modalContainerId;
        return this;
    }

    /**
     * Build the Modal component.
     *
     * @return Modal component ready to render
     * @throws IllegalStateException if required fields are missing
     */
    public Modal build() {
        validateRequiredFields();

        Div modalBody = new Div();

        if (editView != null) {
            Div propertiesSection = new Div()
                    .withClass("edit-properties-section")
                    .withChild(editView);
            modalBody.withChild(propertiesSection);
        }

        Div footer = buildFooter();

        return Modal.create()
                .withTitle(title)
                .withBody(modalBody)
                .withFooter(footer)
                .closeOnBackdrop(false); // Prevent accidental closes
    }

    /**
     * Build the modal footer with action buttons.
     *
     * @return Footer component
     */
    private Div buildFooter() {
        Div footer = new Div().withClass("d-flex justify-content-between w-100");

        // Left side: Delete button (if enabled)
        Div leftButtons = new Div();
        if (showDelete && deleteUrl != null) {
            Button deleteBtn = Button.create("Delete")
                    .withStyle(Button.ButtonStyle.DANGER);

            deleteBtn.withAttribute("hx-delete", deleteUrl);
            deleteBtn.withAttribute("hx-confirm", "Are you sure you want to delete this module? This cannot be undone.");
            deleteBtn.withAttribute("hx-target", "#" + pageContainerId);
            deleteBtn.withAttribute("hx-swap", "none");

            leftButtons.withChild(deleteBtn);
        }
        footer.withChild(leftButtons);

        // Right side: Cancel + Save buttons
        Div rightButtons = new Div().withClass("d-flex gap-2");

        // Cancel button
        Button cancelBtn = Button.create("Cancel")
                .withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("data-modal-id", modalContainerId);
        cancelBtn.withAttribute("onclick", "document.getElementById(this.dataset.modalId).innerHTML = ''");
        rightButtons.withChild(cancelBtn);

        // Save button (use regular button, not submit, so HTMX can intercept)
        Button saveBtn = Button.create("Save Changes")
                .withStyle(Button.ButtonStyle.PRIMARY);

        saveBtn.withAttribute("hx-post", saveUrl);
        saveBtn.withAttribute("hx-swap", "none");
        saveBtn.withAttribute("hx-include", ".modal-body input, .modal-body textarea, .modal-body select");

        rightButtons.withChild(saveBtn);

        footer.withChild(rightButtons);

        return footer;
    }

    /**
     * Validate that all required fields are set.
     *
     * @throws IllegalStateException if validation fails
     */
    private void validateRequiredFields() {
        if (editView == null) {
            throw new IllegalStateException("editView is required");
        }
        if (saveUrl == null) {
            throw new IllegalStateException("saveUrl is required");
        }
    }
}
