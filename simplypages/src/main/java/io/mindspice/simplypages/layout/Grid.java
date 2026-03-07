package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Generic CSS-class driven grid container.
 *
 * <p>Mutable and not thread-safe. The instance stores column and gap state and rewrites class
 * attributes in place when configuration changes; keep instances request-scoped and avoid
 * concurrent reuse.</p>
 */
public class Grid extends HtmlTag {

    protected int columns = 3;
    protected String gap = "medium";
    protected final String baseClass;

    /**
     * Creates a grid with base class {@code grid}, 3 columns, and {@code medium} gap.
     */
    public Grid() {
        this("grid");
    }

    /**
     * Creates a grid with a custom base class.
     *
     * @param baseClass required base class token always retained on updates
     */
    protected Grid(String baseClass) {
        super("div");
        this.baseClass = baseClass;
        this.addClass(baseClass);
        updateClasses();
    }

    /**
     * Factory for a standard grid.
     *
     * @return new grid
     */
    public static Grid create() {
        return new Grid();
    }

    /**
     * Sets column count and rewrites derived classes.
     *
     * @param columns number of columns used in {@code grid-cols-<n>}
     * @return this grid
     */
    public Grid withColumns(int columns) {
        this.columns = columns;
        updateClasses();
        return this;
    }

    /**
     * Sets gap token and rewrites derived classes.
     *
     * @param gap gap token used in {@code gap-<token>}
     * @return this grid
     */
    public Grid withGap(String gap) {
        this.gap = gap;
        updateClasses();
        return this;
    }

    /**
     * Appends one grid item.
     *
     * @param component grid item
     * @return this grid
     */
    public Grid addItem(Component component) {
        super.withChild(component);
        return this;
    }

    /**
     * Appends multiple grid items in argument order.
     *
     * @param components grid items
     * @return this grid
     */
    public Grid addItems(Component... components) {
        for (Component c : components) {
            super.withChild(c);
        }
        return this;
    }

    /**
     * Normalizes and rewrites class attribute to keep one base class plus current
     * {@code grid-cols-*} and {@code gap-*} tokens.
     */
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
