package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Semantic section wrapper for page structure.
 *
 * <p>Mutable and not thread-safe. Configure per request and avoid cross-thread reuse. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 */
public class Section extends HtmlTag {

    /**
     * Creates a section with base class {@code section}.
     */
    public Section() {
        super("section");
        this.withAttribute("class", "section");
    }

    /**
     * Creates a new section instance.
     *
     * @return new section
     */
    public static Section create() {
        return new Section();
    }

    /**
     * Sets the section id.
     *
     * @param id element id
     * @return this section
     */
    public Section withId(String id) {
        this.withAttribute("id", id);
        return this;
    }

    /**
     * Replaces class attribute with {@code section <className>}.
     *
     * @param className additional class token(s)
     * @return this section
     */
    public Section withClass(String className) {
        String currentClass = "section";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    /**
     * Appends a child component.
     *
     * @param component child component
     * @return this section
     */
    @Override
    public Section withChild(Component component) {
        super.withChild(component);
        return this;
    }
}
