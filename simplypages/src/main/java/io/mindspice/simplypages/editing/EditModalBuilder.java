package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.components.forms.Button;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.Component;
import java.util.regex.Pattern;
import java.util.List;

/**
 * Helper for building standardized edit modals.
 *
 * <p>This builder simplifies the creation of edit modals with a consistent
 * layout and behavior. It handles:</p>
 * <ul>
 *   <li>Entity property editing (via {@link Editable#buildEditView()} or {@link EditAdapter#buildEditView()})</li>
 *   <li>Nested child editing (via {@link Editable#getEditableChildren()})</li>
 *   <li>Footer layout (delete on left, cancel/save on right)</li>
 *   <li>HTMX attributes for dynamic updates</li>
 * </ul>
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
    private String childAddUrl;
    private boolean showDelete = true;
    private String pageContainerId = "page-content";
    private String modalContainerId = "edit-modal-container";

    private EditModalBuilder() {
    }

    public static EditModalBuilder create() {
        return new EditModalBuilder();
    }

    public EditModalBuilder withTitle(String title) {
        this.title = title;
        return this;
    }

    public EditModalBuilder withModuleId(String moduleId) {
        this.moduleId = moduleId;
        return this;
    }

    public EditModalBuilder withEditView(Component editView) {
        this.editView = editView;
        return this;
    }

    public EditModalBuilder withEditable(Editable<?> editable) {
        this.editable = editable;
        return this;
    }

    public EditModalBuilder withSaveUrl(String saveUrl) {
        this.saveUrl = saveUrl;
        return this;
    }

    public EditModalBuilder withDeleteUrl(String deleteUrl) {
        this.deleteUrl = deleteUrl;
        return this;
    }

    public EditModalBuilder withChildEditUrl(String childEditUrl) {
        this.childEditUrl = childEditUrl;
        return this;
    }

    public EditModalBuilder withChildDeleteUrl(String childDeleteUrl) {
        this.childDeleteUrl = childDeleteUrl;
        return this;
    }

    public EditModalBuilder withChildAddUrl(String childAddUrl) {
        this.childAddUrl = childAddUrl;
        return this;
    }

    public EditModalBuilder hideDelete() {
        this.showDelete = false;
        return this;
    }

    public EditModalBuilder withPageContainerId(String pageContainerId) {
        if (pageContainerId == null || !VALID_ID_PATTERN.matcher(pageContainerId).matches()) {
            throw new IllegalArgumentException("Invalid page container ID");
        }
        this.pageContainerId = pageContainerId;
        return this;
    }

    public EditModalBuilder withModalContainerId(String modalContainerId) {
        if (modalContainerId == null || !VALID_ID_PATTERN.matcher(modalContainerId).matches()) {
            throw new IllegalArgumentException("Invalid modal container ID");
        }
        this.modalContainerId = modalContainerId;
        return this;
    }

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
        if (editable != null && (!editable.getEditableChildren().isEmpty() || childAddUrl != null)) {
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
        Div headerRow = new Div().withClass("d-flex justify-content-between align-items-center mb-3");
        headerRow.withChild(Header.H4("Content Items").withClass("mb-0"));

        if (childAddUrl != null) {
            Component addBtn = Button.create("Add Item")
                    .withStyle(Button.ButtonStyle.PRIMARY)
                    .small()
                    .withAttribute("hx-get", childAddUrl)
                    .withAttribute("hx-target", "#" + modalContainerId)
                    .withAttribute("hx-swap", "innerHTML");
            headerRow.withChild(addBtn);
        }
        section.withChild(headerRow);

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
