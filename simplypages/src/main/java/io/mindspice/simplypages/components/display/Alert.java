package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Alert message component with severity style variants.
 *
 * <p>Mutable and not thread-safe. Styling methods mutate class attributes on this instance. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
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
        super.addClass("alert-dismissible");
        return this;
    }

    public Alert withClass(String className) {
        super.addClass(className);
        return this;
    }
}
