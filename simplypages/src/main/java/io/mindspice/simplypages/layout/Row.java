package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
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
        this.withAttribute("class", "row gap-" + gap);
        return this;
    }

    /**
     * Replaces class attribute with {@code row align-<alignment>}.
     *
     * @param alignment alignment token used by CSS
     * @return this row
     */
    public Row withAlign(String alignment) {
        String currentClass = "row";
        this.withAttribute("class", currentClass + " align-" + alignment);
        return this;
    }

    /**
     * Replaces class attribute with {@code row justify-<justify>}.
     *
     * @param justify justify token used by CSS
     * @return this row
     */
    public Row withJustify(String justify) {
        String currentClass = "row";
        this.withAttribute("class", currentClass + " justify-" + justify);
        return this;
    }
}
