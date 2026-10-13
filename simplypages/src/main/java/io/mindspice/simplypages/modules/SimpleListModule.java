package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.ListItem;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Module;
import io.mindspice.simplypages.editing.Editable;
import io.mindspice.simplypages.editing.EditableChild;
import io.mindspice.simplypages.editing.FormFieldHelper;
import io.mindspice.simplypages.editing.ValidationResult;

import java.util.*;

/**
 * Editable module that renders a list of {@link ListItem} components.
 *
 * <p>Configure items before building. Adding/removing items changes the model and current
 * editable descriptors, not an already-built child tree. {@link #applyEdits(Map)} explicitly
 * rebuilds that tree. Raw item ids and positional editable descriptor ids are separate:
 * descriptors may use {@code item-N} for unassigned ids and renumber without assigning an id.</p>
 *
 * <p>Configuration is mutable and not thread-safe. Finish configuration before sharing;
 * child delegates and request contexts retain their own sharing requirements.</p>
 */
public class SimpleListModule extends Module implements Editable<SimpleListModule> {

    private final List<ListItem> items = new ArrayList<>();

    public SimpleListModule() {
        super("div");
        this.withClass("simple-list-module");
    }

    public static SimpleListModule create() {
        return new SimpleListModule();
    }

    /** Adds to the item model without building or rebuilding rendered structure. */
    public SimpleListModule addItem(ListItem item) {
        this.items.add(item);
        return this;
    }

    /**
     * Removes every item whose non-null raw id equals the literal query. A null query removes
     * nothing. Unassigned ids and positional editable descriptors are not lookup identities.
     * This changes configuration only, not an already-built child tree.
     */
    public SimpleListModule removeItem(String itemId) {
        if (itemId != null) {
            items.removeIf(item -> itemId.equals(item.getId()));
        }
        return this;
    }

    /**
     * Returns the first item with a non-null raw id exactly equal to the query, or null.
     * Null queries never match; no trimming, normalization or descriptor fallback is performed.
     */
    public ListItem findItem(String itemId) {
        if (itemId == null) {
            return null;
        }
        return items.stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }

    public List<ListItem> getItems() {
        return new ArrayList<>(items);
    }

    @Override
    public SimpleListModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public SimpleListModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H3(title).withClass("module-title"));
        }

        if (items.isEmpty()) {
            super.withChild(new Paragraph("No items yet. Add items to see them here.")
                    .withClass("text-muted"));
        } else {
            HtmlTag ul = new HtmlTag("ul").withClass("list-group");
            for (ListItem item : items) {
                ul.withChild(item);
            }
            super.withChild(ul);
        }
    }

    // ===== Editable Implementation (Module Properties) =====

    @Override
    public Component buildEditView() {
        Div editForm = new Div();
        editForm.withChild(FormFieldHelper.textField("List Title", "title", title));
        return editForm;
    }

    @Override
    public ValidationResult validate(Map<String, String> formData) {
        String title = formData.get("title");
        if (title != null && title.length() > 200) {
            return ValidationResult.invalid("Title must be less than 200 characters");
        }
        return ValidationResult.valid();
    }

    @Override
    public SimpleListModule applyEdits(Map<String, String> formData) {
        if (formData.containsKey("title")) {
            this.title = formData.get("title");
        }

        // Rebuild content
        rebuildContent();

        return this;
    }

    @Override
    public List<EditableChild> getEditableChildren() {
        List<EditableChild> children = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ListItem item = items.get(i);
            String itemId = item.getId() != null ? item.getId() : "item-" + i;
            String label = item.getText() != null && !item.getText().isEmpty() ? item.getText() : "Item " + (i + 1);
            if (label.length() > 30) {
                label = label.substring(0, 27) + "...";
            }
            children.add(EditableChild.create(itemId, label, item));
        }
        return children;
    }

}
