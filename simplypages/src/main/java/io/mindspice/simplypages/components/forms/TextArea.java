package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Multi-line text input control.
 *
 * <p>Mutable and not thread-safe. Mutators update this instance in place. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class TextArea extends HtmlTag {

    /**
     * Creates a textarea with required field name.
     *
     * @param name form field name
     */
    public TextArea(String name) {
        super("textarea");
        this.withAttribute("name", name);
        this.withAttribute("class", "form-textarea");
    }

    /**
     * Creates a textarea.
     *
     * @param name form field name
     * @return new textarea
     */
    public static TextArea create(String name) {
        return new TextArea(name);
    }

    /**
     * Sets id attribute.
     *
     * @param id element id
     * @return this textarea
     */
    @Override
    public TextArea withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Replaces textarea content.
     *
     * @param value textarea content
     * @return this textarea
     */
    public TextArea withValue(String value) {
        this.withInnerText(value);
        return this;
    }

    /**
     * Sets placeholder attribute.
     *
     * @param placeholder placeholder text
     * @return this textarea
     */
    public TextArea withPlaceholder(String placeholder) {
        this.withAttribute("placeholder", placeholder);
        return this;
    }

    /**
     * Sets rows attribute.
     *
     * @param rows row count
     * @return this textarea
     */
    public TextArea withRows(int rows) {
        this.withAttribute("rows", String.valueOf(rows));
        return this;
    }

    /**
     * Sets cols attribute.
     *
     * @param cols column count
     * @return this textarea
     */
    public TextArea withCols(int cols) {
        this.withAttribute("cols", String.valueOf(cols));
        return this;
    }

    /**
     * Marks textarea required.
     *
     * @return this textarea
     */
    public TextArea required() {
        this.withAttribute("required", "");
        return this;
    }

    /**
     * Marks textarea readonly.
     *
     * @return this textarea
     */
    public TextArea readonly() {
        this.withAttribute("readonly", "");
        return this;
    }

    /**
     * Marks textarea disabled.
     *
     * @return this textarea
     */
    public TextArea disabled() {
        this.withAttribute("disabled", "");
        return this;
    }

    /**
     * Sets maximum allowed character count.
     *
     * @param maxLength max length
     * @return this textarea
     */
    public TextArea withMaxLength(int maxLength) {
        this.withAttribute("maxlength", String.valueOf(maxLength));
        return this;
    }

    /**
     * Sets minimum required character count.
     *
     * @param minLength min length
     * @return this textarea
     */
    public TextArea withMinLength(int minLength) {
        this.withAttribute("minlength", String.valueOf(minLength));
        return this;
    }

    /**
     * Replaces class attribute with {@code form-textarea <className>}.
     *
     * @param className additional classes
     * @return this textarea
     */
    public TextArea withClass(String className) {
        String currentClass = "form-textarea";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    /**
     * Sets autofocus attribute.
     *
     * @return this textarea
     */
    public TextArea withAutofocus() {
        this.withAttribute("autofocus", "");
        return this;
    }

    /**
     * Sets wrap behavior.
     *
     * @param wrap wrap mode token
     * @return this textarea
     */
    public TextArea withWrap(String wrap) {
        this.withAttribute("wrap", wrap);
        return this;
    }

    /**
     * Sets inline width style.
     *
     * @param width CSS width value
     * @return this textarea
     */
    @Override
    public TextArea withWidth(String width) {
        super.withWidth(width);
        return this;
    }

    /**
     * Sets inline max-width style.
     *
     * @param maxWidth CSS max-width value
     * @return this textarea
     */
    @Override
    public TextArea withMaxWidth(String maxWidth) {
        super.withMaxWidth(maxWidth);
        return this;
    }

    /**
     * Sets inline min-width style.
     *
     * @param minWidth CSS min-width value
     * @return this textarea
     */
    @Override
    public TextArea withMinWidth(String minWidth) {
        super.withMinWidth(minWidth);
        return this;
    }
}
