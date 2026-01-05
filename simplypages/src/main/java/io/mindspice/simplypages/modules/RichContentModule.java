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
            rebuildContent();
        }
        return this;
    }

}
