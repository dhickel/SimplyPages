package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Single-line input control.
 *
 * <p>Mutable and not thread-safe. Mutators update this element instance in place. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class TextInput extends HtmlTag {

    /**
     * Supported input types mapped to HTML {@code type}.
     */
    public enum InputType {
        TEXT, EMAIL, PASSWORD, NUMBER, TEL, URL, DATE, TIME, DATETIME_LOCAL, SEARCH
    }

    /**
     * Creates a text input with default type {@code text}.
     *
     * @param name form field name
     */
    public TextInput(String name) {
        super("input", true);
        this.withAttribute("type", "text");
        this.withAttribute("name", name);
        this.withAttribute("class", "form-input");
    }

    /**
     * Creates a text input.
     *
     * @param name form field name
     * @return new input
     */
    public static TextInput create(String name) {
        return new TextInput(name);
    }

    /**
     * Creates an email input.
     *
     * @param name form field name
     * @return new input configured as email
     */
    public static TextInput email(String name) {
        return new TextInput(name).withType(InputType.EMAIL);
    }

    /**
     * Creates a password input.
     *
     * @param name form field name
     * @return new input configured as password
     */
    public static TextInput password(String name) {
        return new TextInput(name).withType(InputType.PASSWORD);
    }

    /**
     * Creates a number input.
     *
     * @param name form field name
     * @return new input configured as number
     */
    public static TextInput number(String name) {
        return new TextInput(name).withType(InputType.NUMBER);
    }

    /**
     * Creates a date input.
     *
     * @param name form field name
     * @return new input configured as date
     */
    public static TextInput date(String name) {
        return new TextInput(name).withType(InputType.DATE);
    }

    /**
     * Creates a search input.
     *
     * @param name form field name
     * @return new input configured as search
     */
    public static TextInput search(String name) {
        return new TextInput(name).withType(InputType.SEARCH);
    }

    /**
     * Sets input type.
     *
     * @param type input type enum
     * @return this input
     */
    public TextInput withType(InputType type) {
        this.withAttribute("type", type.name().toLowerCase().replace('_', '-'));
        return this;
    }

    /**
     * Sets id attribute.
     *
     * @param id element id
     * @return this input
     */
    @Override
    public TextInput withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Sets value attribute.
     *
     * @param value input value
     * @return this input
     */
    public TextInput withValue(String value) {
        this.withAttribute("value", value);
        return this;
    }

    /**
     * Sets placeholder attribute.
     *
     * @param placeholder placeholder text
     * @return this input
     */
    public TextInput withPlaceholder(String placeholder) {
        this.withAttribute("placeholder", placeholder);
        return this;
    }

    /**
     * Marks input required.
     *
     * @return this input
     */
    public TextInput required() {
        this.withAttribute("required", "");
        return this;
    }

    /**
     * Marks input readonly.
     *
     * @return this input
     */
    public TextInput readonly() {
        this.withAttribute("readonly", "");
        return this;
    }

    /**
     * Marks input disabled.
     *
     * @return this input
     */
    public TextInput disabled() {
        this.withAttribute("disabled", "");
        return this;
    }

    /**
     * Sets HTML {@code pattern}.
     *
     * @param pattern regex pattern
     * @return this input
     */
    public TextInput withPattern(String pattern) {
        this.withAttribute("pattern", pattern);
        return this;
    }

    /**
     * Sets minimum text length.
     *
     * @param minLength minimum length
     * @return this input
     */
    public TextInput withMinLength(int minLength) {
        this.withAttribute("minlength", String.valueOf(minLength));
        return this;
    }

    /**
     * Sets maximum text length.
     *
     * @param maxLength maximum length
     * @return this input
     */
    public TextInput withMaxLength(int maxLength) {
        this.withAttribute("maxlength", String.valueOf(maxLength));
        return this;
    }

    /**
     * Sets HTML {@code min} value.
     *
     * @param min minimum value token
     * @return this input
     */
    public TextInput withMin(String min) {
        this.withAttribute("min", min);
        return this;
    }

    /**
     * Sets HTML {@code max} value.
     *
     * @param max maximum value token
     * @return this input
     */
    public TextInput withMax(String max) {
        this.withAttribute("max", max);
        return this;
    }

    /**
     * Sets HTML {@code step}.
     *
     * @param step step token
     * @return this input
     */
    public TextInput withStep(String step) {
        this.withAttribute("step", step);
        return this;
    }

    /**
     * Appends class token(s).
     *
     * @param className class token(s)
     * @return this input
     */
    public TextInput withClass(String className) {
        super.addClass(className);
        return this;
    }

    /**
     * Sets autofocus attribute.
     *
     * @return this input
     */
    public TextInput withAutofocus() {
        this.withAttribute("autofocus", "");
        return this;
    }

    /**
     * Sets autocomplete attribute.
     *
     * @param value autocomplete value
     * @return this input
     */
    public TextInput withAutocomplete(String value) {
        this.withAttribute("autocomplete", value);
        return this;
    }

    /**
     * Sets inline width style.
     *
     * @param width CSS width value
     * @return this input
     */
    @Override
    public TextInput withWidth(String width) {
        super.withWidth(width);
        return this;
    }

    /**
     * Sets inline max-width style.
     *
     * @param maxWidth CSS max-width value
     * @return this input
     */
    @Override
    public TextInput withMaxWidth(String maxWidth) {
        super.withMaxWidth(maxWidth);
        return this;
    }

    /**
     * Sets inline min-width style.
     *
     * @param minWidth CSS min-width value
     * @return this input
     */
    @Override
    public TextInput withMinWidth(String minWidth) {
        super.withMinWidth(minWidth);
        return this;
    }
}
