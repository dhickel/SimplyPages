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

    // Exposed for testing
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

        // Generate fields for each child component
        for (int i = 0; i < contentItems.size(); i++) {
            Component item = contentItems.get(i);
            String prefix = "item_" + i + "_";
            Div itemSection = new Div().withClass("edit-item-section");
            itemSection.addStyle("margin-top", "1rem");
            itemSection.addStyle("border-top", "1px solid #ccc");
            itemSection.addStyle("padding-top", "1rem");

            if (item instanceof Paragraph) {
                Paragraph p = (Paragraph) item;
                itemSection.withChild(new Paragraph("Paragraph " + (i + 1)).addStyle("font-weight", "bold"));
                itemSection.withChild(FormFieldHelper.textAreaField("Text", prefix + "text", p.getText(), 3));
            } else if (item instanceof Header) {
                Header h = (Header) item;
                itemSection.withChild(new Paragraph("Header " + (i + 1) + " (" + h.getLevel() + ")").addStyle("font-weight", "bold"));
                itemSection.withChild(FormFieldHelper.textField("Text", prefix + "text", h.getText()));
            } else if (item instanceof Image) {
                Image img = (Image) item;
                itemSection.withChild(new Paragraph("Image " + (i + 1)).addStyle("font-weight", "bold"));
                itemSection.withChild(FormFieldHelper.textField("Source URL", prefix + "src", img.getSrc()));
                itemSection.withChild(FormFieldHelper.textField("Alt Text", prefix + "alt", img.getAlt()));
            } else if (item instanceof Link) {
                Link link = (Link) item;
                itemSection.withChild(new Paragraph("Link " + (i + 1)).addStyle("font-weight", "bold"));
                itemSection.withChild(FormFieldHelper.textField("URL", prefix + "href", link.getHref()));
                itemSection.withChild(FormFieldHelper.textField("Text", prefix + "text", link.getText()));
            }

            editForm.withChild(itemSection);
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

        // Apply edits to children
        for (int i = 0; i < contentItems.size(); i++) {
            Component item = contentItems.get(i);
            String prefix = "item_" + i + "_";

            if (item instanceof Paragraph) {
                String text = formData.get(prefix + "text");
                if (text != null) {
                    // Recreate Paragraph as it might be immutable or we prefer clean state
                    // Actually Paragraph has internal state but let's check its API.
                    // Paragraph has fluent setters but no setText. It has withInnerText but constructor sets text.
                    // Let's replace the item in the list to be safe and consistent.
                    Paragraph oldP = (Paragraph) item;
                    Paragraph newP = new Paragraph(text);
                    // Preserve other properties if needed?
                    if (oldP.getId() != null) newP.withId(oldP.getId());
                    if (oldP.getAlignment() != null) {
                        switch (Paragraph.Alignment.fromCssClass(oldP.getAlignment())) {
                            case LEFT -> newP.left();
                            case CENTER -> newP.center();
                            case RIGHT -> newP.right();
                            case JUSTIFY -> newP.justify();
                        }
                    }
                    contentItems.set(i, newP);
                }
            } else if (item instanceof Header) {
                String text = formData.get(prefix + "text");
                if (text != null) {
                    Header oldH = (Header) item;
                    // Header level is final in our Header class? Let's check.
                    // Header has HeaderLevel level.
                    // We need to preserve level.
                    Header newH = new Header(oldH.getLevel(), text);
                    if (oldH.getId() != null) newH.withId(oldH.getId());
                    contentItems.set(i, newH);
                }
            } else if (item instanceof Image) {
                String src = formData.get(prefix + "src");
                String alt = formData.get(prefix + "alt");
                if (src != null) {
                    Image oldImg = (Image) item;
                    Image newImg = Image.create(src, alt != null ? alt : oldImg.getAlt());
                    if (oldImg.getId() != null) newImg.withId(oldImg.getId());
                    if (oldImg.getWidth() != null) newImg.withSize(oldImg.getWidth(), oldImg.getHeight());
                    contentItems.set(i, newImg);
                }
            } else if (item instanceof Link) {
                String href = formData.get(prefix + "href");
                String text = formData.get(prefix + "text");
                if (href != null && text != null) {
                    Link oldLink = (Link) item;
                    Link newLink = Link.create(href, text);
                    if (oldLink.getId() != null) newLink.withId(oldLink.getId());
                    // Preserve HTMX props if any? Ideally yes but this is a simple implementation.
                    contentItems.set(i, newLink);
                }
            }
        }

        rebuildContent();
        return this;
    }

}
