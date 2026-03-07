package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Small inline badge component with style presets.
 *
 * <p>Mutable and not thread-safe. Style/shape mutators replace class attributes in place. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class Badge extends HtmlTag {

    public enum BadgeStyle {
        PRIMARY("badge-primary"),
        SECONDARY("badge-secondary"),
        SUCCESS("badge-success"),
        DANGER("badge-danger"),
        WARNING("badge-warning"),
        INFO("badge-info");

        private final String cssClass;

        BadgeStyle(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }
    }

    public Badge(String text) {
        super("span");
        this.withAttribute("class", "badge badge-primary");
        this.withInnerText(text);
    }

    public static Badge create(String text) {
        return new Badge(text);
    }

    public static Badge primary(String text) {
        return new Badge(text).withStyle(BadgeStyle.PRIMARY);
    }

    public static Badge secondary(String text) {
        return new Badge(text).withStyle(BadgeStyle.SECONDARY);
    }

    public static Badge success(String text) {
        return new Badge(text).withStyle(BadgeStyle.SUCCESS);
    }

    public static Badge danger(String text) {
        return new Badge(text).withStyle(BadgeStyle.DANGER);
    }

    public static Badge warning(String text) {
        return new Badge(text).withStyle(BadgeStyle.WARNING);
    }

    public static Badge info(String text) {
        return new Badge(text).withStyle(BadgeStyle.INFO);
    }

    public Badge withStyle(BadgeStyle style) {
        this.withAttribute("class", "badge " + style.getCssClass());
        return this;
    }

    public Badge pill() {
        String currentClass = "badge";
        this.withAttribute("class", currentClass + " badge-pill");
        return this;
    }

    public Badge withClass(String className) {
        super.addClass(className);
        return this;
    }
}
