package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Minimal fluent wrapper around an HTML {@code div}.
 *
 * <p>Mutable and not thread-safe. Each mutator updates this instance in place. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class Div extends HtmlTag {

    /**
     * Creates an empty div.
     */
    public Div() {
        super("div");
    }

    /**
     * Adds class token(s) without replacing previously assigned classes.
     *
     * @param className class token(s)
     * @return this div
     */
    public Div withClass(String className) {
        super.withClass(className);
        return this;
    }

    /**
     * Sets id attribute.
     *
     * @param id element id
     * @return this div
     */
    @Override
    public Div withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Sets or replaces an attribute.
     *
     * @param name attribute name
     * @param value attribute value
     * @return this div
     */
    @Override
    public Div withAttribute(String name, String value) {
        super.withAttribute(name, value);
        return this;
    }

    /**
     * Appends a child component.
     *
     * @param component child component
     * @return this div
     */
    @Override
    public Div withChild(Component component) {
        super.withChild(component);
        return this;
    }
}
