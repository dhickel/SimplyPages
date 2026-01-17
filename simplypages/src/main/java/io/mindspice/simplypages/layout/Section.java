package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Section component for semantic page sections.
 */
public class Section extends HtmlTag {

    public Section() {
        super("section");
        this.withAttribute("class", "section");
    }

    public static Section create() {
        return new Section();
    }

    public Section withId(String id) {
        this.withAttribute("id", id);
        return this;
    }

    public Section withClass(String className) {
        String currentClass = "section";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public Section withChild(Component component) {
        super.withChild(component);
        return this;
    }
}
