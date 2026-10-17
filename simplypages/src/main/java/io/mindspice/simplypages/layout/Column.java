package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.Arrays;

/**
 * Grid column for use inside {@link Row}.
 *
 * <p>Mutable and not thread-safe while being configured. Once configuration is complete,
 * standard rendering reads the structure without changing it. Shared children and custom
 * render overrides must also be safe to share.</p>
 */
public class Column extends HtmlTag {

    /**
     * Creates a column element with its base layout class already present.
     */
    public Column() {
        super("div");
        super.withAttribute("class", "col");
    }

    /**
     * Creates a new column.
     *
     * @return new column
     */
    public static Column create() {
        return new Column();
    }

    /**
     * Sets fixed column width in the 12-column layout system.
     *
     * @param width width token from 1 to 12
     * @return this column
     * @throws IllegalArgumentException when width is outside {@code [1,12]}
     */
    public Column withWidth(int width) {
        if (width < 1 || width > 12) {
            throw new IllegalArgumentException("Column width must be between 1 and 12");
        }
        this.withAttribute("class", "col col-" + width);
        return this;
    }

    /**
     * Sets auto-sized column behavior via {@code col-auto}.
     *
     * @return this column
     */
    public Column auto() {
        this.withAttribute("class", "col col-auto");
        return this;
    }

    /**
     * Sets fill behavior via {@code col-fill}.
     *
     * @return this column
     */
    public Column fill() {
        this.withAttribute("class", "col col-fill");
        return this;
    }

    /**
     * Sets id attribute.
     *
     * @param id element id
     * @return this column
     */
    @Override
    public Column withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Replaces class attribute with {@code col <className>}.
     *
     * @param className additional class token(s)
     * @return this column
     */
    public Column withClass(String className) {
        String currentClass = "col";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    /**
     * Appends a child component.
     *
     * @param component child component
     * @return this column
     */
    @Override
    public Column withChild(Component component) {
        super.withChild(component);
        return this;
    }

    /**
     * Appends each child component in argument order.
     *
     * @param components components to append
     * @return this column
     */
    public Column withChildren(Component... components) {
        for (Component comp : components) {
            super.withChild(comp);
        }
        return this;
    }

    /**
     * Adds or replaces an attribute, preserving the base {@code col} token when setting classes.
     *
     * @param name attribute name
     * @param value attribute value
     * @return this column
     */
    @Override
    public HtmlTag withAttribute(String name, String value) {
        if ("class".equalsIgnoreCase(name)) {
            if (value == null || value.isBlank()) {
                value = "col";
            } else if (Arrays.stream(value.split("\\s+")).noneMatch("col"::equals)) {
                value += " col";
            }
        }
        return super.withAttribute(name, value);
    }
}
