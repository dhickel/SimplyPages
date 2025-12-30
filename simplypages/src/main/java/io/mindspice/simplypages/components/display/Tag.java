package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Tag component for categorization and labeling.
 * Similar to Badge but typically used for categories/keywords.
 */
public class Tag extends HtmlTag {

    public Tag(String text) {
        super("span");
        this.withAttribute("class", "tag");
        this.withInnerText(text);
    }

    public static Tag create(String text) {
        return new Tag(text);
    }

    public Tag withColor(String color) {
        this.withAttribute("class", "tag tag-" + color);
        return this;
    }

    public Tag removable() {
        String currentClass = "tag";
        this.withAttribute("class", currentClass + " tag-removable");
        return this;
    }

    public Tag withClass(String className) {
        String currentClass = "tag";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }
}
