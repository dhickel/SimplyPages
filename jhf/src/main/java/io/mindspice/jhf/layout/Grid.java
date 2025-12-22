package io.mindspice.jhf.layout;

import io.mindspice.jhf.core.Attribute;
import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.List;

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
    }

    public static Grid create() {
        return new Grid();
    }

    public Grid withColumns(int columns) {
        this.columns = columns;
        return this;
    }

    public Grid withGap(String gap) {
        this.gap = gap;
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

    @Override
    public String render() {
        // Logic to update grid classes based on state
        // We need to remove old grid-cols/gap classes to prevent accumulation if re-rendered
        // But we can't easily find "old" classes without regex on the current class string.

        String currentClass = attributes.stream()
                .filter(attr -> "class".equals(attr.getName()))
                .map(Attribute::getValue)
                .findFirst()
                .orElse("");

        // Clean up old grid layout classes
        currentClass = currentClass.replaceAll("grid-cols-\\d+", "").replaceAll("gap-\\w+", "").trim();

        // Ensure base class is present if it was somehow removed (unlikely but safe)
        // Actually, we trust baseClass is there or user removed it intentionally.
        // But let's follow the pattern of adding layout classes.

        String newClass = currentClass + " grid-cols-" + columns + " gap-" + gap;

        // Update the attribute directly to replace the cleaned string
        this.withAttribute("class", newClass);

        return super.render();
    }
}
