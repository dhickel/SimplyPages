package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Paragraph component with alignment class management helpers.
 *
 * <p>Text/alignment configuration is mutable and must finish before sharing. Rendering derives
 * alignment classes without changing stored attributes or children. Compiled templates retain
 * this renderer as a live delegate, so later configuration is reflected in their output.
 * Custom child renderers and request contexts retain their own sharing requirements.</p>
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
    @Override
    public Paragraph withId(String id) {
        super.withId(id);
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
     * Sets the alignment used by the temporary rendering tag.
     */
    private void setAlignmentInternal(String newAlignment) {
        this.alignment = newAlignment;
    }

    @Override
    public String render(RenderContext context) {
        RenderTag output = new RenderTag();
        if (alignment != null) {
            updateAlignmentClass(output);
        }
        return output.render(context);
    }

    @Override
    public String render() {
        return render(RenderContext.empty());
    }

    private final class RenderTag extends HtmlTag {
        private RenderTag() {
            super(Paragraph.this.tagName, Paragraph.this.selfClosing);
            attributes.addAll(Paragraph.this.attributes);
            id = Paragraph.this.id;
            innerText = Paragraph.this.innerText;
            innerTextSlot = Paragraph.this.innerTextSlot;
            trustedHtml = Paragraph.this.trustedHtml;
        }

        @Override
        protected Stream<Component> getChildrenStream() {
            return Paragraph.this.getChildrenStream();
        }
    }

    private void updateAlignmentClass(HtmlTag output) {
        Optional<Attribute> classAttr = attributes.stream()
            .filter(attr -> "class".equalsIgnoreCase(attr.name()))
            .findFirst();

        List<String> classes = new ArrayList<>();
        if (classAttr.isPresent()) {
            String current = classAttr.get().value();
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
            output.withAttribute("class", String.join(" ", classes));
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
