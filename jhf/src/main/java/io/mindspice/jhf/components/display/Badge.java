package io.mindspice.jhf.components.display;

import io.mindspice.jhf.core.HtmlTag;

/**
 * Badge component for displaying small labels and counts.
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
        String currentClass = attributes.stream()
            .filter(attr -> "class".equals(attr.getName()))
            .findFirst()
            .map(attr -> attr.getValue())
            .orElse("badge badge-primary");
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }
}
