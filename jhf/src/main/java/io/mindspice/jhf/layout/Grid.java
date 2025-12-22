package io.mindspice.jhf.layout;

import io.mindspice.jhf.core.Attribute;
import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Grid layout for creating responsive grid layouts.
 */
public class Grid extends HtmlTag {

    private int columns = 3;
    private String gap = "medium";
    private final List<Component> items = new ArrayList<>();

    public Grid() {
        super("div");
        this.withAttribute("class", "grid");
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
        items.add(component);
        return this;
    }

    public Grid addItems(Component... components) {
        items.addAll(List.of(components));
        return this;
    }

    public Grid withClass(String className) {
        String currentClass = "grid";
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public String render() {
        String currentClass = attributes.stream()
                .filter(attr -> "class".equals(attr.getName()))
                .map(Attribute::getValue)
                .findFirst()
                .orElse("grid");

        // Ensure base grid class is present and add layout classes
        if (!currentClass.contains("grid")) {
            currentClass = "grid " + currentClass;
        }

        // Remove old grid-cols/gap classes to prevent accumulation if re-rendered
        currentClass = currentClass.replaceAll("grid-cols-\\d+", "").replaceAll("gap-\\w+", "").trim();

        this.withAttribute("class", currentClass + " grid-cols-" + columns + " gap-" + gap);
        items.forEach(item -> super.withChild(item));
        return super.render();
    }
}
