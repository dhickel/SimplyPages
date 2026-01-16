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
 * Simple list module demonstrating container pattern with list items.
 *
 * <p>This module contains a list of {@link ListItem} components. Demonstrates:</p>
 * <ul>
 *   <li>Module-level properties (title)</li>
 *   <li>List item rendering</li>
 *   <li>Programmatic item management</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>
 * SimpleListModule module = SimpleListModule.create()
 *     .withTitle("My List")
 *     .addItem(ListItem.create("First item"))
 *     .addItem(ListItem.create("Second item"));
 * </pre>
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

    public SimpleListModule addItem(ListItem item) {
        this.items.add(item);
        return this;
    }

    public SimpleListModule removeItem(String itemId) {
        items.removeIf(item -> item.getId().equals(itemId));
        return this;
    }

    public ListItem findItem(String itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
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
            String id = item.getId();
            if (id == null) {
                id = "item-" + i;
            }
            String title = "Item " + (i + 1);
            children.add(new EditableChild(id, item, title));
        }
        return children;
    }

}
