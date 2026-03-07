package io.mindspice.simplypages.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable builder for CSS utility classes and inline declarations.
 *
 * <p>This type stores tokens only; it does not validate CSS semantics and does not escape values.
 * Consumers are responsible for supplying trusted class and style input.</p>
 *
 * <p>Mutability/thread-safety: mutable and not thread-safe. Use per component/request and do not
 * share across concurrent renders.</p>
 */
public class Style {
    /** Accumulated class tokens in insertion order. */
    private final List<String> classes = new ArrayList<>();
    /** Accumulated inline style fragments in insertion order. */
    private final List<String> inlineStyles = new ArrayList<>();

    /**
     * Creates a new empty style builder.
     */
    public static Style create() {
        return new Style();
    }

    /** Adds a utility class token in the form {@code p-{value}}. */
    public Style padding(String value) {
        classes.add("p-" + value);
        return this;
    }

    /** Adds a utility class token in the form {@code m-{value}}. */
    public Style margin(String value) {
        classes.add("m-" + value);
        return this;
    }

    /** Adds a utility class token in the form {@code text-{alignment}}. */
    public Style textAlign(String alignment) {
        classes.add("text-" + alignment);
        return this;
    }

    /** Adds a utility class token in the form {@code text-{size}}. */
    public Style fontSize(String size) {
        classes.add("text-" + size);
        return this;
    }

    /** Adds a utility class token in the form {@code font-{weight}}. */
    public Style fontWeight(String weight) {
        classes.add("font-" + weight);
        return this;
    }

    /** Adds a utility class token in the form {@code text-{color}}. */
    public Style color(String color) {
        classes.add("text-" + color);
        return this;
    }

    /** Adds a utility class token in the form {@code bg-{color}}. */
    public Style backgroundColor(String color) {
        classes.add("bg-" + color);
        return this;
    }

    /** Adds a utility class token in the form {@code border-{value}}. */
    public Style border(String value) {
        classes.add("border-" + value);
        return this;
    }

    /** Adds a utility class token in the form {@code rounded-{value}}. */
    public Style rounded(String value) {
        classes.add("rounded-" + value);
        return this;
    }

    /** Adds a utility class token in the form {@code w-{value}}. */
    public Style width(String value) {
        classes.add("w-" + value);
        return this;
    }

    /** Adds a utility class token in the form {@code h-{value}}. */
    public Style height(String value) {
        classes.add("h-" + value);
        return this;
    }

    /** Adds the {@code flex} utility class. */
    public Style flex() {
        classes.add("flex");
        return this;
    }

    /** Adds a utility class token in the form {@code flex-{direction}}. */
    public Style flexDirection(String direction) {
        classes.add("flex-" + direction);
        return this;
    }

    /** Adds a utility class token in the form {@code justify-{value}}. */
    public Style justifyContent(String value) {
        classes.add("justify-" + value);
        return this;
    }

    /** Adds a utility class token in the form {@code items-{value}}. */
    public Style alignItems(String value) {
        classes.add("items-" + value);
        return this;
    }

    /** Adds a utility class token in the form {@code gap-{value}}. */
    public Style gap(String value) {
        classes.add("gap-" + value);
        return this;
    }

    /** Adds a utility class token in the form {@code shadow-{value}}. */
    public Style shadow(String value) {
        classes.add("shadow-" + value);
        return this;
    }

    /** Adds a raw class token without transformation. */
    public Style addClass(String className) {
        classes.add(className);
        return this;
    }

    /** Adds raw class tokens without transformation. */
    public Style addClasses(String... classNames) {
        classes.addAll(List.of(classNames));
        return this;
    }

    /**
     * Appends an inline style fragment in the form {@code property: value}.
     *
     * <p>No CSS sanitization is performed.</p>
     */
    public Style addInlineStyle(String property, String value) {
        inlineStyles.add(property + ": " + value);
        return this;
    }

    /** Returns class tokens joined by a single space. */
    public String getClassString() {
        return String.join(" ", classes);
    }

    /** Returns inline style fragments joined by {@code ; }. */
    public String getStyleString() {
        return String.join("; ", inlineStyles);
    }

    /** Returns whether at least one class token has been added. */
    public boolean hasClasses() {
        return !classes.isEmpty();
    }

    /** Returns whether at least one inline style fragment has been added. */
    public boolean hasInlineStyles() {
        return !inlineStyles.isEmpty();
    }
}
