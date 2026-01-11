package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Image;
import io.mindspice.simplypages.components.Paragraph;
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

    public List<Component> getContentItems() {
        return Collections.unmodifiableList(contentItems);
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

        // Iterate through existing content items and generate edit fields
        for (int i = 0; i < contentItems.size(); i++) {
            Component item = contentItems.get(i);
            String prefix = "item_" + i + "_";

            if (item instanceof Paragraph p) {
                editForm.withChild(FormFieldHelper.hiddenField(prefix + "type", "PARAGRAPH"));
                editForm.withChild(FormFieldHelper.textAreaField("Paragraph " + (i + 1), prefix + "text", p.getText(), 3));
            } else if (item instanceof Header h) {
                editForm.withChild(FormFieldHelper.hiddenField(prefix + "type", "HEADER"));
                editForm.withChild(FormFieldHelper.hiddenField(prefix + "level", h.getLevel().name()));
                editForm.withChild(FormFieldHelper.textField("Header " + (i + 1), prefix + "text", h.getText()));
            } else if (item instanceof Image img) {
                editForm.withChild(FormFieldHelper.hiddenField(prefix + "type", "IMAGE"));
                editForm.withChild(FormFieldHelper.textField("Image Source " + (i + 1), prefix + "src", img.getSrc()));
                editForm.withChild(FormFieldHelper.textField("Alt Text", prefix + "alt", img.getAlt()));
            } else if (item instanceof Link l) {
                editForm.withChild(FormFieldHelper.hiddenField(prefix + "type", "LINK"));
                editForm.withChild(FormFieldHelper.textField("Link Text " + (i + 1), prefix + "text", l.getText()));
                editForm.withChild(FormFieldHelper.textField("URL", prefix + "href", l.getHref()));
            }
        }

        return editForm;
    }

    @Override
    public ValidationResult validate(Map<String, String> formData) {
        String titleValue = formData.get("title");

        if (titleValue == null || titleValue.trim().isEmpty()) {
            return ValidationResult.invalid("Module title cannot be empty");
        }

        return ValidationResult.valid();
    }

    @Override
    public RichContentModule applyEdits(Map<String, String> formData) {
        if (formData.containsKey("title")) {
            this.title = formData.get("title");
        }

        // Reconstruct content items
        List<Component> newItems = new ArrayList<>();
        int index = 0;
        while (true) {
            String prefix = "item_" + index + "_";
            if (!formData.containsKey(prefix + "type")) {
                break; // No more items
            }

            String type = formData.get(prefix + "type");
            switch (type) {
                case "PARAGRAPH":
                    String pText = formData.get(prefix + "text");
                    newItems.add(new Paragraph(pText != null ? pText : ""));
                    break;
                case "HEADER":
                    String hText = formData.get(prefix + "text");
                    String levelStr = formData.get(prefix + "level");
                    Header.HeaderLevel level = Header.HeaderLevel.valueOf(levelStr);
                    newItems.add(new Header(level, hText != null ? hText : ""));
                    break;
                case "IMAGE":
                    String src = formData.get(prefix + "src");
                    String alt = formData.get(prefix + "alt");
                    newItems.add(new Image(src != null ? src : "", alt != null ? alt : ""));
                    break;
                case "LINK":
                    String lText = formData.get(prefix + "text");
                    String href = formData.get(prefix + "href");
                    newItems.add(new Link(href != null ? href : "", lText != null ? lText : ""));
                    break;
            }
            index++;
        }

        if (!newItems.isEmpty()) {
            this.contentItems.clear();
            this.contentItems.addAll(newItems);
        }

        rebuildContent();
        return this;
    }

}
