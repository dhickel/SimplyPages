package io.mindspice.jhf.components;

import io.mindspice.jhf.core.HtmlTag;

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
    }

    private String alignment;

    public Paragraph(String text) {
        super("p");
        this.withInnerText(text);
    }

    public Paragraph() {
        super("p");
    }

    public Paragraph withClass(String className) {
        this.withAttribute("class", className);
        return this;
    }

    /**
     * Aligns paragraph text to the left (default).
     */
    public Paragraph left() {
        this.alignment = Alignment.LEFT.getCssClass();
        return this;
    }

    /**
     * Aligns paragraph text to the center.
     */
    public Paragraph center() {
        this.alignment = Alignment.CENTER.getCssClass();
        return this;
    }

    /**
     * Aligns paragraph text to the right.
     */
    public Paragraph right() {
        this.alignment = Alignment.RIGHT.getCssClass();
        return this;
    }

    /**
     * Justifies paragraph text.
     */
    public Paragraph justify() {
        this.alignment = Alignment.JUSTIFY.getCssClass();
        return this;
    }

    @Override
    public String render() {
        // Apply alignment if set
        if (alignment != null) {
            boolean hasClass = attributes.stream()
                .anyMatch(attr -> "class".equals(attr.getName()));

            if (hasClass) {
                for (int i = 0; i < attributes.size(); i++) {
                    if ("class".equals(attributes.get(i).getName())) {
                        String currentClass = attributes.get(i).getValue();
                        attributes.remove(i);
                        this.withAttribute("class", currentClass + " " + alignment);
                        break;
                    }
                }
            } else {
                this.withAttribute("class", alignment);
            }
        }
        return super.render();
    }
}
