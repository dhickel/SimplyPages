package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.CssClassNames;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.Arrays;

/**
 * Horizontal layout container for the framework row/column grid.
 *
 * <p>This type is mutable and not thread-safe. Configure and render within a single request
 * (or other confined lifecycle) and do not share one instance across concurrent threads.</p>
 *
 * <p>Non-{@link Column} children passed to {@link #withChild(Component)} are wrapped in a
 * default {@code div.col} to preserve row semantics.</p>
 */
public class Row extends HtmlTag {
    private static final java.util.Set<String> GAP_TOKENS = java.util.Set.of("sm", "medium", "lg");
    private static final java.util.Set<String> ALIGN_TOKENS = java.util.Set.of("start", "end", "center", "baseline", "stretch");
    private static final java.util.Set<String> JUSTIFY_TOKENS = java.util.Set.of("start", "end", "center", "between", "around", "evenly");

    /**
     * Creates an empty row with base class {@code row}.
     */
    public Row() {
        super("div");
        this.withAttribute("class", "row");
    }

    /**
     * Appends all components to this row in argument order.
     *
     * @param components components to append
     * @return this row
     */
    public Row withComponents(Component... components) {
        Arrays.stream(components).forEach(this::withChild);
        return this;
    }

    /**
     * Sets the {@code id} attribute.
     *
     * @param id element id
     * @return this row
     */
    @Override
    public Row withId(String id) {
        super.withId(id);
        return this;
    }

    /**
     * Appends a child component.
     *
     * <p>If {@code component} is a {@link Column}, it is appended directly; otherwise it is
     * wrapped in {@code div.col} before append.</p>
     *
     * @param component component to append
     * @return this row
     */
    @Override
    public Row withChild(Component component) {
        // If it's already a Column, add it directly
        if (component instanceof Column) {
            super.withChild(component);
        } else {
            // Wrap children in a 'col' div for styling
            Div col = new Div().withClass("col").withChild(component);
            super.withChild(col);
        }
        return this;
    }

    /**
     * Appends a preconfigured {@link Column} without additional wrapping.
     *
     * @param column column to append
     * @return this row
     */
    public Row addColumn(Column column) {
        super.withChild(column);
        return this;
    }

    /**
     * Replaces class attribute with {@code row gap-<gap>}.
     *
     * @param gap gap token used by CSS
     * @return this row
     */
    public Row withGap(String gap) {
        String normalized = normalizeToken(gap, GAP_TOKENS, "gap");
        CssClassNames.replacePrefixed(this, "row", "gap-" + normalized, "gap-");
        return this;
    }

    /**
     * Replaces class attribute with {@code row align-<alignment>}.
     *
     * @param alignment alignment token used by CSS
     * @return this row
     */
    public Row withAlign(String alignment) {
        String normalized = normalizeToken(alignment, ALIGN_TOKENS, "alignment");
        CssClassNames.replacePrefixed(this, "row", "items-" + normalized, "items-", "align-");
        return this;
    }

    /**
     * Replaces class attribute with {@code row justify-<justify>}.
     *
     * @param justify justify token used by CSS
     * @return this row
     */
    public Row withJustify(String justify) {
        String normalized = normalizeToken(justify, JUSTIFY_TOKENS, "justify");
        CssClassNames.replacePrefixed(this, "row", "justify-" + normalized, "justify-");
        return this;
    }

    @Override
    public Row withClass(String className) {
        CssClassNames.addTokens(this, "row", className);
        return this;
    }

    private String normalizeToken(String token, java.util.Set<String> allowed, String label) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Row " + label + " token cannot be blank");
        }
        String normalized = token.trim().toLowerCase();
        if ("small".equals(normalized)) {
            normalized = "sm";
        } else if ("large".equals(normalized)) {
            normalized = "lg";
        }
        if (!allowed.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported row " + label + " token: " + token);
        }
        return normalized;
    }
}
