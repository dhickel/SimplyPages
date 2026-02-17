package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Column component for controlling column width and behavior in a Row.
 */
public class Column extends HtmlTag {

    public Column() {
        super("div");
        // Don't set class here - let withWidth/withClass handle it
    }

    public static Column create() {
        return new Column();
    }

    /**
     * Set specific column width (1-12 for 12-column grid)
     */
    public Column withWidth(int width) {
        if (width < 1 || width > 12) {
            throw new IllegalArgumentException("Column width must be between 1 and 12");
        }
        this.withAttribute("class", "col col-" + width);
        return this;
    }

    /**
     * Set column to auto width
     */
    public Column auto() {
        this.withAttribute("class", "col col-auto");
        return this;
    }

    /**
     * Set column to take remaining space
     */
    public Column fill() {
        this.withAttribute("class", "col col-fill");
        return this;
    }

    @Override
    public Column withId(String id) {
        super.withId(id);
        return this;
    }

    public Column withClass(String className) {
        String currentClass = "col";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public Column withChild(Component component) {
        super.withChild(component);
        return this;
    }

    public Column withChildren(Component... components) {
        for (Component comp : components) {
            super.withChild(comp);
        }
        return this;
    }

    @Override
    public String render() {
        // Ensure "col" class is present before rendering
        ensureColClass();
        return super.render();
    }

    /**
     * Ensures the "col" class is present on this component.
     * This is called automatically before rendering.
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
