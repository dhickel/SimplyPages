package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.CssClassNames;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.Set;

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
    private static final Set<String> GAP_TOKENS = Set.of("sm", "medium", "lg");

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
        if (columns < 1 || columns > 6) {
            throw new IllegalArgumentException("Grid columns must be between 1 and 6");
        }
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
        this.gap = normalizeGap(gap);
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

        CssClassNames.replaceMatching(
            this,
            baseClass,
            "grid-cols-" + columns + " gap-" + gap,
            token -> token.matches("grid-cols-\\d+") || token.startsWith("gap-")
        );
    }

    private String normalizeGap(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Grid gap token cannot be blank");
        }
        String normalized = token.trim().toLowerCase();
        if ("small".equals(normalized)) {
            normalized = "sm";
        } else if ("large".equals(normalized)) {
            normalized = "lg";
        }
        if (!GAP_TOKENS.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported grid gap token: " + token);
        }
        return normalized;
    }
}
