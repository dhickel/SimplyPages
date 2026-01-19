package io.mindspice.simplypages.layout;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.Arrays;

public class Row extends HtmlTag {

    public Row() {
        super("div");
        this.withAttribute("class", "row");
    }

    public Row withComponents(Component... components) {
        Arrays.stream(components).forEach(this::withChild);
        return this;
    }

    @Override
    public Row withId(String id) {
        super.withId(id);
        return this;
    }
    
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

    public Row addColumn(Column column) {
        super.withChild(column);
        return this;
    }

    public Row withGap(String gap) {
        this.withAttribute("class", "row gap-" + gap);
        return this;
    }

    public Row withAlign(String alignment) {
        String currentClass = "row";
        this.withAttribute("class", currentClass + " align-" + alignment);
        return this;
    }

    public Row withJustify(String justify) {
        String currentClass = "row";
        this.withAttribute("class", currentClass + " justify-" + justify);
        return this;
    }
}
