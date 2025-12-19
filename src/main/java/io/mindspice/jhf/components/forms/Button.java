package io.mindspice.jhf.components.forms;

import io.mindspice.jhf.core.HtmlTag;

/**
 * Button component with various styles and types.
 */
public class Button extends HtmlTag {

    public enum ButtonType {
        BUTTON, SUBMIT, RESET
    }

    public enum ButtonStyle {
        PRIMARY("btn-primary"),
        SECONDARY("btn-secondary"),
        SUCCESS("btn-success"),
        DANGER("btn-danger"),
        WARNING("btn-warning"),
        INFO("btn-info"),
        LINK("btn-link");

        private final String cssClass;

        ButtonStyle(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    public Button(String text) {
        super("button");
        this.withAttribute("type", "button");
        this.withAttribute("class", "btn btn-primary");
        this.withInnerText(text);
    }

    public static Button create(String text) {
        return new Button(text);
    }

    public static Button submit(String text) {
        return new Button(text).withType(ButtonType.SUBMIT);
    }

    public static Button reset(String text) {
        return new Button(text).withType(ButtonType.RESET);
    }

    public Button withType(ButtonType type) {
        this.withAttribute("type", type.name().toLowerCase());
        return this;
    }

    public Button withStyle(ButtonStyle style) {
        this.withAttribute("class", "btn " + style.getCssClass());
        return this;
    }

    public Button withId(String id) {
        this.withAttribute("id", id);
        return this;
    }

    public Button disabled() {
        this.withAttribute("disabled", "");
        return this;
    }

    public Button withClass(String className) {
        String currentClass = attributes.stream()
            .filter(attr -> attr.toString().contains("class="))
            .findFirst()
            .map(attr -> attr.toString().split("=")[1].replaceAll("\"", "").trim())
            .orElse("btn btn-primary");
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    public Button withOnClick(String onClick) {
        this.withAttribute("onclick", onClick);
        return this;
    }

    public Button fullWidth() {
        return this.withClass("btn-full-width");
    }

    public Button large() {
        return this.withClass("btn-lg");
    }

    public Button small() {
        return this.withClass("btn-sm");
    }

    @Override
    public Button withWidth(String width) {
        super.withWidth(width);
        return this;
    }

    @Override
    public Button withMaxWidth(String maxWidth) {
        super.withMaxWidth(maxWidth);
        return this;
    }

    @Override
    public Button withMinWidth(String minWidth) {
        super.withMinWidth(minWidth);
        return this;
    }
}
