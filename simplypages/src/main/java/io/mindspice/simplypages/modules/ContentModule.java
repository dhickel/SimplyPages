package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Markdown;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.editing.Editable;
import io.mindspice.simplypages.editing.FormFieldHelper;
import io.mindspice.simplypages.editing.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Editable module for rendering text or custom content blocks.
 *
 * <p>Contract: when {@code customContent} is set it takes precedence; otherwise {@code content}
 * is rendered as Markdown by default or as escaped text when markdown is disabled.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Edit flows mutate the same
 * instance and call {@code rebuildContent()} to satisfy module build-once lifecycle semantics;
 * mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class ContentModule extends Module implements Editable<ContentModule> {

    private String content;
    private boolean useMarkdown = true;
    private Component customContent;

    /** Creates an empty content module. */
    public ContentModule() {
        super("div");
        this.withClass("content-module");
    }

    /** Creates a new module instance. */
    public static ContentModule create() {
        return new ContentModule();
    }

    /** Sets text/markdown source content. */
    public ContentModule withContent(String content) {
        this.content = content;
        return this;
    }

    /** Sets explicit custom content and bypasses text rendering when present. */
    public ContentModule withCustomContent(Component content) {
        this.customContent = content;
        return this;
    }

    /** Disables Markdown rendering and renders content as escaped text. */
    public ContentModule disableMarkdown() {
        this.useMarkdown = false;
        return this;
    }

    @Override
    public ContentModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public ContentModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        HtmlTag contentWrapper = new HtmlTag("div").withAttribute("class", "module-content");

        if (customContent != null) {
            contentWrapper.withChild(customContent);
        } else if (content != null) {
            if (useMarkdown) {
                contentWrapper.withChild(new Markdown(content));
            } else {
                contentWrapper.withInnerText(content);
            }
        }

        super.withChild(contentWrapper);
    }

    /** {@inheritDoc} */
    @Override
    public Component buildEditView() {
        Div editForm = new Div();
        editForm.withChild(FormFieldHelper.textField("Title", "title", title));
        editForm.withChild(FormFieldHelper.textAreaField("Content", "content", content, 15));
        editForm.withChild(FormFieldHelper.checkboxField("Render as Markdown", "useMarkdown", useMarkdown));
        return editForm;
    }

    /** {@inheritDoc} */
    @Override
    public ContentModule applyEdits(Map<String, String> formData) {
        if (formData.containsKey("title")) {
            this.title = formData.get("title");
        }
        if (formData.containsKey("content")) {
            this.content = formData.get("content");
        }
        this.useMarkdown = formData.containsKey("useMarkdown");

        // CRITICAL FIX: Module lifecycle is build-once, so we must clear and rebuild
        // when content changes
        rebuildContent();

        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ValidationResult validate(Map<String, String> formData) {
        List<String> errors = new ArrayList<>();

        String title = formData.get("title");
        if (title != null && title.length() > 200) {
            errors.add("Title must be less than 200 characters");
        }

        String content = formData.get("content");
        if (content == null || content.trim().isEmpty()) {
            errors.add("Content cannot be empty");
        }

        return errors.isEmpty() ?
                ValidationResult.valid() :
                ValidationResult.invalid(errors);
    }
}
