package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Grid column for use inside {@link Row}.
 *
 * <p>Mutable and not thread-safe. Width/class mutators rewrite the same instance and should be
 * used in a request-scoped build/render flow.</p>
 */
public class Column extends HtmlTag {

    /**
     * Creates a column element. Base class is added lazily before render if missing.
     */
    public Column() {
        super("div");
        // Don't set class here - let withWidth/withClass handle it
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
     * Ensures a {@code col} class exists before rendering.
     *
     * @return rendered HTML
     */
    @Override
    public String render() {
        // Ensure "col" class is present before rendering
        ensureColClass();
        return super.render();
    }

    /**
     * Ensures the {@code col} class token is present on the class attribute.
     */
    private void ensureColClass() {
        // Check if any class attribute exists
        boolean hasClassAttr = attributes.stream()
                .anyMatch(attr -> "class".equals(attr.name()));

        if (!hasClassAttr) {
            // No class attribute - add "col"
            this.withAttribute("class", "col");
        } else {
            // Check if "col" class is already in the class list
            boolean hasColClass = attributes.stream()
                    .filter(attr -> "class".equals(attr.name()))
                    .anyMatch(attr -> attr.value().contains("col"));

            if (!hasColClass) {
                // Find and update the class attribute
                for (Attribute attr : attributes) {
                    if ("class".equals(attr.name())) {
                        // Prepend "col" to existing classes
                        String newValue = "col " + attr.value();
                        attributes.remove(attr);
                        this.withAttribute("class", newValue);
                        break;
                    }
                }
            }
        }
    }
}
