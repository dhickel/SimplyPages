package io.mindspice.simplypages.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent API for building CSS styles and classes.
 * Provides type-safe way to add styling to components.
 */
public class Style {
    private final List<String> classes = new ArrayList<>();
    private final List<String> inlineStyles = new ArrayList<>();

    public static Style create() {
        return new Style();
    }

    // Utility classes
    public Style padding(String value) {
        classes.add("p-" + value);
        return this;
    }

    public Style margin(String value) {
        classes.add("m-" + value);
        return this;
    }

    public Style textAlign(String alignment) {
        classes.add("text-" + alignment);
        return this;
    }

    public Style fontSize(String size) {
        classes.add("text-" + size);
        return this;
    }

    public Style fontWeight(String weight) {
        classes.add("font-" + weight);
        return this;
    }

    public Style color(String color) {
        classes.add("text-" + color);
        return this;
    }

    public Style backgroundColor(String color) {
        classes.add("bg-" + color);
        return this;
    }

    public Style border(String value) {
        classes.add("border-" + value);
        return this;
    }

    public Style rounded(String value) {
        classes.add("rounded-" + value);
        return this;
    }

    public Style width(String value) {
        classes.add("w-" + value);
        return this;
    }

    public Style height(String value) {
        classes.add("h-" + value);
        return this;
    }

    public Style flex() {
        classes.add("flex");
        return this;
    }

    public Style flexDirection(String direction) {
        classes.add("flex-" + direction);
        return this;
    }

    public Style justifyContent(String value) {
        classes.add("justify-" + value);
        return this;
    }

    public Style alignItems(String value) {
        classes.add("items-" + value);
        return this;
    }

    public Style gap(String value) {
        classes.add("gap-" + value);
        return this;
    }

    public Style shadow(String value) {
        classes.add("shadow-" + value);
        return this;
    }

    // Add custom class
    public Style addClass(String className) {
        classes.add(className);
        return this;
    }

    // Add multiple custom classes
    public Style addClasses(String... classNames) {
        classes.addAll(List.of(classNames));
        return this;
    }

    // Inline style (use sparingly, prefer classes)
    public Style addInlineStyle(String property, String value) {
        inlineStyles.add(property + ": " + value);
        return this;
    }

    public String getClassString() {
        return String.join(" ", classes);
    }

    public String getStyleString() {
        return String.join("; ", inlineStyles);
    }

    public boolean hasClasses() {
        return !classes.isEmpty();
    }

    public boolean hasInlineStyles() {
        return !inlineStyles.isEmpty();
    }
}
