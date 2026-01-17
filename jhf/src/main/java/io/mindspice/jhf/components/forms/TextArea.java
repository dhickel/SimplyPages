package io.mindspice.jhf.components.forms;

import io.mindspice.jhf.core.HtmlTag;

/**
 * TextArea component for multi-line text input.
 */
public class TextArea extends HtmlTag {

    public TextArea(String name) {
        super("textarea");
        this.withAttribute("name", name);
        this.withAttribute("class", "form-textarea");
    }

    public static TextArea create(String name) {
        return new TextArea(name);
    }

    public TextArea withId(String id) {
        this.withAttribute("id", id);
        return this;
    }

    public TextArea withValue(String value) {
        this.withInnerText(value);
        return this;
    }

    public TextArea withPlaceholder(String placeholder) {
        this.withAttribute("placeholder", placeholder);
        return this;
    }

    public TextArea withRows(int rows) {
        this.withAttribute("rows", String.valueOf(rows));
        return this;
    }

    public TextArea withCols(int cols) {
        this.withAttribute("cols", String.valueOf(cols));
        return this;
    }

    public TextArea required() {
        this.withAttribute("required", "");
        return this;
    }

    public TextArea readonly() {
        this.withAttribute("readonly", "");
        return this;
    }

    public TextArea disabled() {
        this.withAttribute("disabled", "");
        return this;
    }

    public TextArea withMaxLength(int maxLength) {
        this.withAttribute("maxlength", String.valueOf(maxLength));
        return this;
    }

    public TextArea withMinLength(int minLength) {
        this.withAttribute("minlength", String.valueOf(minLength));
        return this;
    }

    public TextArea withClass(String className) {
        String currentClass = "form-textarea";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    public TextArea withAutofocus() {
        this.withAttribute("autofocus", "");
        return this;
    }

    public TextArea withWrap(String wrap) {
        this.withAttribute("wrap", wrap);
        return this;
    }

    @Override
    public TextArea withWidth(String width) {
        super.withWidth(width);
        return this;
    }

    @Override
    public TextArea withMaxWidth(String maxWidth) {
        super.withMaxWidth(maxWidth);
        return this;
    }

    @Override
    public TextArea withMinWidth(String minWidth) {
        super.withMinWidth(minWidth);
        return this;
    }
}
