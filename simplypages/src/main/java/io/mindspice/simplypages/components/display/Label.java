package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Text label component for form/input association.
 *
 * <p>Mutable and not thread-safe. Attribute/class methods update this instance in place. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class Label extends HtmlTag {

    public Label(String text) {
        super("label");
        this.withAttribute("class", "label");
        this.withInnerText(text);
    }

    public static Label create(String text) {
        return new Label(text);
    }

    public Label forInput(String inputId) {
        this.withAttribute("for", inputId);
        return this;
    }

    public Label withClass(String className) {
        String currentClass = "label";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    public Label required() {
        return this.withClass("label-required");
    }
}
