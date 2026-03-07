package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.Component;
import java.util.regex.Pattern;
import java.util.List;

/**
 * Builds standardized edit modals for module property and child-edit flows.
 *
 * <p>Contract: exactly one edit source must be provided ({@code editView} or {@code editable}).
 * Save actions post to {@code saveUrl}; child actions are emitted only when corresponding URL
 * templates are configured.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Configure and build within a
 * single request flow.</p>
 */
public class EditModalBuilder {

    private static final Pattern VALID_ID_PATTERN =
        Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]*$");

    private String title = "Edit Module";
    private String moduleId;
    private Component editView;
    private Editable<?> editable;
    private String saveUrl;
    private String deleteUrl;
    private String childEditUrl;
    private String childDeleteUrl;
    private boolean showDelete = true;
    private String pageContainerId = "page-content";
    private String modalContainerId = "edit-modal-container";

    private EditModalBuilder() {
    }

    /**
     * Creates a new builder instance.
     */
    public static EditModalBuilder create() {
        return new EditModalBuilder();
    }

    /** Sets modal title text. */
    public EditModalBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    /** Sets the module identifier used by callers for endpoint construction. */
    public EditModalBuilder withModuleId(String moduleId) {
        this.moduleId = moduleId;
        return this;
    }

    /** Sets pre-built edit view content for main module properties. */
    public EditModalBuilder withEditView(Component editView) {
        this.editView = editView;
        return this;
    }

    /** Sets editable module adapter used to source edit view and child handles. */
    public EditModalBuilder withEditable(Editable<?> editable) {
        this.editable = editable;
        return this;
    }

    /** Sets endpoint used by the Save action button. */
    public EditModalBuilder withSaveUrl(String saveUrl) {
        this.saveUrl = saveUrl;
        return this;
    }

    /** Sets endpoint used by the Delete action button. */
    public EditModalBuilder withDeleteUrl(String deleteUrl) {
        this.deleteUrl = deleteUrl;
        return this;
    }

    /** Sets child-edit URL template; {@code {id}} is replaced per child. */
    public EditModalBuilder withChildEditUrl(String childEditUrl) {
        this.childEditUrl = childEditUrl;
        return this;
    }

    /** Sets child-delete URL template; {@code {id}} is replaced per child. */
    public EditModalBuilder withChildDeleteUrl(String childDeleteUrl) {
        this.childDeleteUrl = childDeleteUrl;
        return this;
    }

    /** Hides the delete action in the modal footer. */
    public EditModalBuilder hideDelete() {
        this.showDelete = false;
        return this;
    }

    /**
     * Sets page container id targeted by delete operations.
     *
     * @throws IllegalArgumentException when id is null or fails identifier validation
     */
    public EditModalBuilder withPageContainerId(String pageContainerId) {
        if (pageContainerId == null || !VALID_ID_PATTERN.matcher(pageContainerId).matches()) {
            throw new IllegalArgumentException("Invalid page container ID");
        }
        this.pageContainerId = pageContainerId;
        return this;
    }

    /**
     * Sets modal container id targeted by modal updates.
     *
     * @throws IllegalArgumentException when id is null or fails identifier validation
     */
    public EditModalBuilder withModalContainerId(String modalContainerId) {
        if (modalContainerId == null || !VALID_ID_PATTERN.matcher(modalContainerId).matches()) {
            throw new IllegalArgumentException("Invalid modal container ID");
        }
        this.modalContainerId = modalContainerId;
        return this;
    }

    /**
     * Builds the configured modal.
     *
     * @throws IllegalStateException when required edit source or save URL is missing
     */
    public Modal build() {
        // Use editable's view if editView not manually set
        if (editView == null && editable != null) {
            this.editView = editable.buildEditView();
        }

        validateRequiredFields();

        Div modalBody = new Div();

        // Main properties section
        if (editView != null) {
            Div propertiesSection = new Div()
                    .withClass("edit-properties-section")
                    .withChild(editView);
            modalBody.withChild(propertiesSection);
        }

        // Child editing section (if applicable)
        if (editable != null && !editable.getEditableChildren().isEmpty()) {
            Div childrenSection = buildChildrenSection();
            modalBody.withChild(childrenSection);
        }

        Div footer = buildFooter();

        return Modal.create()
                .withTitle(title)
                .withBody(modalBody)
                .withFooter(footer)
                .closeOnBackdrop(false);
    }

    private Div buildChildrenSection() {
        Div section = new Div().withClass("edit-children-section mt-4");
        section.withChild(Header.H4("Content Items").withClass("mb-3"));

        Div list = new Div().withClass("list-group");

        for (EditableChild child : editable.getEditableChildren()) {
             Div item = new Div()
                    .withClass("list-group-item d-flex justify-content-between align-items-center p-2");

             Component label = new Div().withInnerText(child.getLabel() != null ? child.getLabel() : "Item " + child.getId());
             item.withChild(label);

             Div actions = new Div().withClass("btn-group btn-group-sm");

             if (childEditUrl != null) {
                 String url = childEditUrl.replace("{id}", child.getId());
                 Component editBtn = Button.create("Edit")
                         .withStyle(Button.ButtonStyle.SECONDARY)
                         .withAttribute("hx-get", url)
                         .withAttribute("hx-target", "#" + modalContainerId)
                         .withAttribute("hx-swap", "innerHTML");
                 actions.withChild(editBtn);
             }

             if (childDeleteUrl != null) {
                 String url = childDeleteUrl.replace("{id}", child.getId());
                  Component deleteBtn = Button.create("Delete")
                         .withStyle(Button.ButtonStyle.DANGER)
                         .withAttribute("hx-delete", url)
                         .withAttribute("hx-confirm", "Delete this item?")
                         .withAttribute("hx-target", "#" + modalContainerId);
                 actions.withChild(deleteBtn);
             }

             item.withChild(actions);
             list.withChild(item);
        }

        section.withChild(list);
        return section;
    }

    private Div buildFooter() {
        Div footer = new Div().withClass("d-flex justify-content-between w-100");

        Div leftButtons = new Div();
        if (showDelete && deleteUrl != null) {
            Button deleteBtn = Button.create("Delete Module")
                    .withStyle(Button.ButtonStyle.DANGER);
            deleteBtn.withAttribute("hx-delete", deleteUrl);
            deleteBtn.withAttribute("hx-confirm", "Are you sure you want to delete this module?");
            deleteBtn.withAttribute("hx-target", "#" + pageContainerId);
            deleteBtn.withAttribute("hx-swap", "none");
            leftButtons.withChild(deleteBtn);
        }
        footer.withChild(leftButtons);

        Div rightButtons = new Div().withClass("d-flex gap-2");

        Button cancelBtn = Button.create("Close")
                .withStyle(Button.ButtonStyle.SECONDARY);
        cancelBtn.withAttribute("data-modal-id", modalContainerId);
        cancelBtn.withAttribute("onclick", "document.getElementById(this.dataset.modalId).innerHTML = ''");
        rightButtons.withChild(cancelBtn);

        Button saveBtn = Button.create("Save Properties")
                .withStyle(Button.ButtonStyle.PRIMARY);
        saveBtn.withAttribute("hx-post", saveUrl);
        saveBtn.withAttribute("hx-swap", "none");
        // Only include inputs from properties section
        saveBtn.withAttribute("hx-include", ".edit-properties-section input, .edit-properties-section textarea, .edit-properties-section select");

        rightButtons.withChild(saveBtn);
        footer.withChild(rightButtons);

        return footer;
    }

    private void validateRequiredFields() {
        if (editView == null && editable == null) {
            throw new IllegalStateException("Either editView or editable must be provided");
        }
        if (saveUrl == null) {
            throw new IllegalStateException("saveUrl is required");
        }
    }
}
