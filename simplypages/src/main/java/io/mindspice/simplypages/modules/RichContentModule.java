package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.forms.TextInput;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.Module;
import io.mindspice.simplypages.editing.EditAdapter;
import io.mindspice.simplypages.editing.FormFieldHelper;
import io.mindspice.simplypages.editing.ValidationResult;

import java.util.*;

/**
 * Rich content module that can contain paragraphs, images, and links.
 * Demonstrates container module pattern with multiple component types as children.
 */
public class RichContentModule extends Module implements EditAdapter<RichContentModule> {

    private final List<Component> contentItems = new ArrayList<>();
    private String addChildActionUrl; // URL to post to for adding children (HTMX)

    public RichContentModule(String title) {
        super("div");
        this.title = title;
    }

    public static RichContentModule create(String title) {
        return new RichContentModule(title);
    }

    public RichContentModule withTitle(String title) {
        this.title = title;
        return this;
    }

    public RichContentModule withAddChildActionUrl(String url) {
        this.addChildActionUrl = url;
        return this;
    }

    /**
     * @deprecated Use {@link #withModuleId(String)} for fluent configuration.
     */
    @Deprecated
    public RichContentModule setModuleId(String id) {
        this.withModuleId(id);
        return this;
    }

    public String getModuleId() {
        return this.moduleId;
    }

    public RichContentModule addParagraph(Paragraph paragraph) {
        contentItems.add(paragraph);
        return this;
    }

    public RichContentModule addImage(Image image) {
        contentItems.add(image);
        return this;
    }

    public RichContentModule addLink(Link link) {
        contentItems.add(link);
        return this;
    }

    public RichContentModule addHeader(Header header) {
        contentItems.add(header);
        return this;
    }

    @Override
    protected void buildContent() {
        // Build module structure
        Div moduleContainer = new Div().withClass("module rich-content-module");

        if (title != null && !title.isEmpty()) {
            moduleContainer.withChild(new Paragraph(title).withClass("module-title"));
        }

        Div contentContainer = new Div().withClass("content-container");
        for (Component item : contentItems) {
            Div itemWrapper = new Div().withClass("content-item");
            itemWrapper.withChild(item);
            contentContainer.withChild(itemWrapper);
        }

        moduleContainer.withChild(contentContainer);
        super.withChild(moduleContainer);
    }

    // EditAdapter implementation
    @Override
    public Component buildEditView() {
        Div editForm = new Div();
        editForm.withChild(FormFieldHelper.textField("Module Title", "title", title));

        editForm.withChild(new Header(Header.HeaderLevel.H4, "Content Items").withClass("mt-3 mb-2"));

        for (int i = 0; i < contentItems.size(); i++) {
            Component item = contentItems.get(i);
            Div itemGroup = new Div().withClass("item-edit-group p-3 border mb-3 rounded");
            itemGroup.withChild(hiddenField("item_" + i + "_index", String.valueOf(i)));

            if (item instanceof Paragraph p) {
                itemGroup.withChild(new Header(Header.HeaderLevel.H5, "Paragraph " + (i + 1)));
                itemGroup.withChild(hiddenField("item_" + i + "_type", "paragraph"));
                itemGroup.withChild(FormFieldHelper.textAreaField("Text", "item_" + i + "_text", p.getText(), 3));
                itemGroup.withChild(FormFieldHelper.textField("Alignment", "item_" + i + "_align", p.getAlignment()));
            } else if (item instanceof Header h) {
                itemGroup.withChild(new Header(Header.HeaderLevel.H5, "Header " + (i + 1)));
                itemGroup.withChild(hiddenField("item_" + i + "_type", "header"));
                itemGroup.withChild(FormFieldHelper.textField("Text", "item_" + i + "_text", h.getText()));
                itemGroup.withChild(FormFieldHelper.textField("Level", "item_" + i + "_level", h.getLevel().name()));
            } else if (item instanceof Image img) {
                itemGroup.withChild(new Header(Header.HeaderLevel.H5, "Image " + (i + 1)));
                itemGroup.withChild(hiddenField("item_" + i + "_type", "image"));
                itemGroup.withChild(FormFieldHelper.textField("Source URL", "item_" + i + "_src", img.getSrc()));
                itemGroup.withChild(FormFieldHelper.textField("Alt Text", "item_" + i + "_alt", img.getAlt()));
            } else if (item instanceof Link link) {
                itemGroup.withChild(new Header(Header.HeaderLevel.H5, "Link " + (i + 1)));
                itemGroup.withChild(hiddenField("item_" + i + "_type", "link"));
                itemGroup.withChild(FormFieldHelper.textField("URL", "item_" + i + "_href", link.getHref()));
                itemGroup.withChild(FormFieldHelper.textField("Text", "item_" + i + "_text", link.getText()));
            }

            editForm.withChild(itemGroup);
        }

        // Add item count to help parsing
        editForm.withChild(hiddenField("item_count", String.valueOf(contentItems.size())));

        // Add child buttons if action URL is configured
        if (addChildActionUrl != null) {
            Div buttonGroup = new Div().withClass("d-flex gap-2 mt-3 p-3 border-top");
            buttonGroup.withChild(createAddButton("Add Paragraph", "add_paragraph"));
            buttonGroup.withChild(createAddButton("Add Header", "add_header"));
            buttonGroup.withChild(createAddButton("Add Image", "add_image"));
            buttonGroup.withChild(createAddButton("Add Link", "add_link"));
            editForm.withChild(buttonGroup);
        }

        return editForm;
    }

    private Component createAddButton(String label, String action) {
        // We use a regular button that triggers a POST to the action URL
        // The controller should save current state, add the item, and re-render the modal
        return new io.mindspice.simplypages.components.forms.Button(label)
                .withClass("btn btn-sm btn-secondary")
                .withAttribute("hx-post", addChildActionUrl + "?action=" + action)
                .withAttribute("hx-target", "#edit-modal-container") // Refresh the whole modal
                .withAttribute("hx-include", "closest form"); // Include current form data to persist edits
    }

    private Component hiddenField(String name, String value) {
        return new TextInput(name)
                .withValue(value != null ? value : "")
                .withAttribute("type", "hidden");
    }

    @Override
    public ValidationResult validate(Map<String, String> formData) {
        List<String> errors = new ArrayList<>();

        String titleValue = formData.get("title");
        if (titleValue == null || titleValue.trim().isEmpty()) {
            errors.add("Module title cannot be empty");
        }

        // Validate children
        int count = parseItemCount(formData);
        for (int i = 0; i < count; i++) {
            String type = formData.get("item_" + i + "_type");
            if (type == null) continue; // Should not happen if count is correct

            if ("image".equals(type)) {
                String src = formData.get("item_" + i + "_src");
                if (src != null && src.trim().toLowerCase().startsWith("javascript:")) {
                    errors.add("Item " + (i + 1) + ": Javascript URLs are not allowed");
                }
            } else if ("link".equals(type)) {
                String href = formData.get("item_" + i + "_href");
                if (href != null && href.trim().toLowerCase().startsWith("javascript:")) {
                    errors.add("Item " + (i + 1) + ": Javascript URLs are not allowed");
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.valid() : ValidationResult.invalid(errors);
    }

    @Override
    public RichContentModule applyEdits(Map<String, String> formData) {
        if (formData.containsKey("title")) {
            this.title = formData.get("title");
        }

        // Reconstruct children
        contentItems.clear();
        int count = parseItemCount(formData);

        for (int i = 0; i < count; i++) {
            String type = formData.get("item_" + i + "_type");
            if (type == null) continue;

            switch (type) {
                case "paragraph" -> {
                    String text = formData.getOrDefault("item_" + i + "_text", "");
                    String align = formData.get("item_" + i + "_align");
                    Paragraph p = new Paragraph(text);
                    if (align != null) {
                        // Apply alignment if valid
                        switch (align) {
                            case "align-center" -> p.center();
                            case "align-right" -> p.right();
                            case "align-justify" -> p.justify();
                            default -> p.left();
                        }
                    }
                    contentItems.add(p);
                }
                case "header" -> {
                    String text = formData.getOrDefault("item_" + i + "_text", "");
                    String levelStr = formData.getOrDefault("item_" + i + "_level", "H2");
                    Header.HeaderLevel level;
                    try {
                        level = Header.HeaderLevel.valueOf(levelStr);
                    } catch (IllegalArgumentException e) {
                        level = Header.HeaderLevel.H2;
                    }
                    contentItems.add(new Header(level, text));
                }
                case "image" -> {
                    String src = formData.getOrDefault("item_" + i + "_src", "");
                    String alt = formData.getOrDefault("item_" + i + "_alt", "");
                    contentItems.add(new Image(src, alt));
                }
                case "link" -> {
                    String href = formData.getOrDefault("item_" + i + "_href", "#");
                    String text = formData.getOrDefault("item_" + i + "_text", "");
                    contentItems.add(new Link(href, text));
                }
            }
        }

        rebuildContent();
        return this;
    }

    private int parseItemCount(Map<String, String> formData) {
        String countStr = formData.get("item_count");
        if (countStr != null) {
            try {
                return Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        // Fallback: try to find max index
        int max = -1;
        for (String key : formData.keySet()) {
            if (key.startsWith("item_") && key.endsWith("_type")) {
                try {
                    String[] parts = key.split("_");
                    if (parts.length >= 2) {
                        int idx = Integer.parseInt(parts[1]);
                        if (idx > max) max = idx;
                    }
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return max + 1;
    }

}
