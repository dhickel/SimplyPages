package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Paragraph component for text content with alignment options.
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Basic paragraph
 * new Paragraph("Some text");
 *
 * // Centered paragraph
 * new Paragraph("Centered text").center();
 *
 * // Right-aligned paragraph
 * new Paragraph().withInnerText("Right aligned").right();
 * }</pre>
 */
public class Paragraph extends HtmlTag {

    public enum Alignment {
        LEFT("align-left"),
        CENTER("align-center"),
        RIGHT("align-right"),
        JUSTIFY("align-justify");

        private final String cssClass;

        Alignment(String cssClass) {
            this.cssClass = cssClass;
        }

        public String getCssClass() {
            return cssClass;
        }

        public static Alignment fromCssClass(String cssClass) {
            for (Alignment a : values()) {
                if (a.cssClass.equals(cssClass)) {
                    return a;
                }
            }
            return LEFT;
        }
    }

    private String id;  // Optional - only applied to DOM if set
    private String text;
    private String alignment;

    public Paragraph(String text) {
        super("p");
        this.text = text;
        this.withInnerText(text);
    }

    public Paragraph() {
        super("p");
        this.text = "";
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getAlignment() {
        return alignment;
    }

    // Fluent setters
    /**
     * Sets the HTML id attribute for this paragraph.
     *
     * @param id the HTML id attribute value
     * @return this Paragraph for method chaining
     */
    public Paragraph withId(String id) {
        this.id = id;
        if (id != null) {
            this.withAttribute("id", id);
        }
        return this;
    }

    public Paragraph withClass(String className) {
        this.withAttribute("class", className);
        return this;
    }

    /**
     * Aligns paragraph text to the left (default).
     */
    public Paragraph left() {
        setAlignmentInternal(Alignment.LEFT.getCssClass());
        return this;
    }

    /**
     * Aligns paragraph text to the center.
     */
    public Paragraph center() {
        setAlignmentInternal(Alignment.CENTER.getCssClass());
        return this;
    }

    /**
     * Aligns paragraph text to the right.
     */
    public Paragraph right() {
        setAlignmentInternal(Alignment.RIGHT.getCssClass());
        return this;
    }

    /**
     * Justifies paragraph text.
     */
    public Paragraph justify() {
        setAlignmentInternal(Alignment.JUSTIFY.getCssClass());
        return this;
    }

    /**
     * Internal method to set alignment and reset applied flag.
     */
    private void setAlignmentInternal(String newAlignment) {
        this.alignment = newAlignment;
    }

    @Override
    public String render() {
        if (alignment != null) {
            updateAlignmentClass();
        }
        return super.render();
    }

    private void updateAlignmentClass() {
        Optional<Attribute> classAttr = attributes.stream()
            .filter(attr -> "class".equals(attr.getName()))
            .findFirst();

        List<String> classes = new ArrayList<>();
        if (classAttr.isPresent()) {
            String current = classAttr.get().getValue();
            if (current != null && !current.isBlank()) {
                for (String token : current.trim().split("\\s+")) {
                    if (!isAlignmentClass(token)) {
                        classes.add(token);
                    }
                }
            }
        }

        if (!classes.contains(alignment)) {
            classes.add(alignment);
        }

        if (!classes.isEmpty()) {
            this.withAttribute("class", String.join(" ", classes));
        }
    }

    private boolean isAlignmentClass(String className) {
        for (Alignment value : Alignment.values()) {
            if (value.getCssClass().equals(className)) {
                return true;
            }
        }
        return false;
    }

}
