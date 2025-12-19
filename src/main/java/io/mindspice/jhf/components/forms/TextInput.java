package io.mindspice.jhf.components.forms;

import io.mindspice.jhf.core.HtmlTag;

/**
 * Text input component for single-line text entry.
 * Supports various input types (text, email, password, number, etc.)
 */
public class TextInput extends HtmlTag {

    public enum InputType {
        TEXT, EMAIL, PASSWORD, NUMBER, TEL, URL, DATE, TIME, DATETIME_LOCAL, SEARCH
    }

    public TextInput(String name) {
        super("input", true);
        this.withAttribute("type", "text");
        this.withAttribute("name", name);
        this.withAttribute("class", "form-input");
    }

    public static TextInput create(String name) {
        return new TextInput(name);
    }

    public static TextInput email(String name) {
        return new TextInput(name).withType(InputType.EMAIL);
    }

    public static TextInput password(String name) {
        return new TextInput(name).withType(InputType.PASSWORD);
    }

    public static TextInput number(String name) {
        return new TextInput(name).withType(InputType.NUMBER);
    }

    public static TextInput date(String name) {
        return new TextInput(name).withType(InputType.DATE);
    }

    public static TextInput search(String name) {
        return new TextInput(name).withType(InputType.SEARCH);
    }

    public TextInput withType(InputType type) {
        this.withAttribute("type", type.name().toLowerCase().replace('_', '-'));
        return this;
    }

    public TextInput withId(String id) {
        this.withAttribute("id", id);
        return this;
    }

    public TextInput withValue(String value) {
        this.withAttribute("value", value);
        return this;
    }

    public TextInput withPlaceholder(String placeholder) {
        this.withAttribute("placeholder", placeholder);
        return this;
    }

    public TextInput required() {
        this.withAttribute("required", "");
        return this;
    }

    public TextInput readonly() {
        this.withAttribute("readonly", "");
        return this;
    }

    public TextInput disabled() {
        this.withAttribute("disabled", "");
        return this;
    }

    public TextInput withPattern(String pattern) {
        this.withAttribute("pattern", pattern);
        return this;
    }

    public TextInput withMinLength(int minLength) {
        this.withAttribute("minlength", String.valueOf(minLength));
        return this;
    }

    public TextInput withMaxLength(int maxLength) {
        this.withAttribute("maxlength", String.valueOf(maxLength));
        return this;
    }

    public TextInput withMin(String min) {
        this.withAttribute("min", min);
        return this;
    }

    public TextInput withMax(String max) {
        this.withAttribute("max", max);
        return this;
    }

    public TextInput withStep(String step) {
        this.withAttribute("step", step);
        return this;
    }

    public TextInput withClass(String className) {
        String currentClass = attributes.stream()
            .filter(attr -> "class".equals(attr.toString().split("=")[0].trim()))
            .findFirst()
            .map(attr -> attr.toString().split("=")[1].replaceAll("\"", "").trim())
            .orElse("form-input");
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    public TextInput withAutofocus() {
        this.withAttribute("autofocus", "");
        return this;
    }

    public TextInput withAutocomplete(String value) {
        this.withAttribute("autocomplete", value);
        return this;
    }

    @Override
    public TextInput withWidth(String width) {
        super.withWidth(width);
        return this;
    }

    @Override
    public TextInput withMaxWidth(String maxWidth) {
        super.withMaxWidth(maxWidth);
        return this;
    }

    @Override
    public TextInput withMinWidth(String minWidth) {
        super.withMinWidth(minWidth);
        return this;
    }
}
