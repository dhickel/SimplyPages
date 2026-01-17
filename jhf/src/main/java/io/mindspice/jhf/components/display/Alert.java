package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Alert component for displaying notifications and messages.
 */
public class Alert extends HtmlTag {

    public enum AlertType {
        INFO("alert-info"),
        SUCCESS("alert-success"),
        WARNING("alert-warning"),
        DANGER("alert-danger");

        private final String cssClass;

        AlertType(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    public Alert(String message, AlertType type) {
        super("div");
        this.withAttribute("class", "alert " + type.getCssClass());
        this.withInnerText(message);
    }

    public static Alert info(String message) {
        return new Alert(message, AlertType.INFO);
    }

    public static Alert success(String message) {
        return new Alert(message, AlertType.SUCCESS);
    }

    public static Alert warning(String message) {
        return new Alert(message, AlertType.WARNING);
    }

    public static Alert danger(String message) {
        return new Alert(message, AlertType.DANGER);
    }

    public Alert dismissible() {
        String currentClass = attributes.stream()
            .filter(attr -> "class".equals(attr.getName()))
            .findFirst()
            .map(attr -> attr.getValue())
            .orElse("alert");
        this.withAttribute("class", currentClass + " alert-dismissible");
        return this;
    }

    public Alert withClass(String className) {
        String currentClass = attributes.stream()
            .filter(attr -> "class".equals(attr.getName()))
            .findFirst()
            .map(attr -> attr.getValue())
            .orElse("alert");
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }
}
