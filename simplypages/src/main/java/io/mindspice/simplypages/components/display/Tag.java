package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Tag/keyword label component.
 *
 * <p>Mutable and not thread-safe. Color/removable helpers rewrite class attributes in place. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
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
