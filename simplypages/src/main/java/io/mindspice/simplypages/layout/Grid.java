package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Grid layout for creating responsive grid layouts.
 */
public class Grid extends HtmlTag {

    protected int columns = 3;
    protected String gap = "medium";
    protected final String baseClass;

    public Grid() {
        this("grid");
    }

    protected Grid(String baseClass) {
        super("div");
        this.baseClass = baseClass;
        this.addClass(baseClass);
        updateClasses();
    }

    public static Grid create() {
        return new Grid();
    }

    public Grid withColumns(int columns) {
        this.columns = columns;
        updateClasses();
        return this;
    }

    public Grid withGap(String gap) {
        this.gap = gap;
        updateClasses();
        return this;
    }

    public Grid addItem(Component component) {
        super.withChild(component);
        return this;
    }

    public Grid addItems(Component... components) {
        for (Component c : components) {
            super.withChild(c);
        }
        return this;
    }

    private void updateClasses() {
        // Logic to update grid classes based on state
        // We need to remove old grid-cols/gap classes to prevent accumulation.

        String currentClass = attributes.stream()
                .filter(attr -> "class".equals(attr.name()))
                .map(Attribute::value)
                .findFirst()
                .orElse("");

        // Clean up old grid layout classes
        currentClass = currentClass.replaceAll("grid-cols-\\d+", "").replaceAll("gap-\\w+", "").trim();

        // Remove extra spaces potentially created by replaceAll
        currentClass = currentClass.replaceAll("\\s+", " ");

        // Rebuild class string
        // Ensure base class is present if not
        if (!currentClass.contains(baseClass)) {
            currentClass = (baseClass + " " + currentClass).trim();
        }

        String newClass = (currentClass + " grid-cols-" + columns + " gap-" + gap).trim();

        // Update the attribute directly
        this.withAttribute("class", newClass);
    }
}
