package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Attribute;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Header component for text headings with alignment and decoration options.
 *
 * <p>Headers support H1-H6 levels, alignment, and optional top/bottom dividers.</p>
 */
public class Header extends HtmlTag {

    public enum HeaderLevel {
        H1, H2, H3, H4, H5, H6
    }

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

    private String id;  // Optional - only applied to DOM if set
    private HeaderLevel level;
    private String text;
    private Component topBar;
    private Component bottomBar;
    private String alignment;

    public Header(HeaderLevel level, String text) {
        super(level.name().toLowerCase());
        this.level = level;
        this.text = text;
        this.withInnerText(text);
    }

    public static Header H1(String text) {
        return new Header(HeaderLevel.H1, text);
    }

    public static Header H2(String text) {
        return new Header(HeaderLevel.H2, text);
    }

    public static Header H3(String text) {
        return new Header(HeaderLevel.H3, text);
    }

    public static Header H4(String text) {
        return new Header(HeaderLevel.H4, text);
    }

    public static Header H5(String text) {
        return new Header(HeaderLevel.H5, text);
    }

    public static Header H6(String text) {
        return new Header(HeaderLevel.H6, text);
    }

    public String getId() {
        return id;
    }

    /**
     * Sets the HTML id attribute for this header.
     * <p>
     * When set, this id will be applied to the DOM element, allowing
     * targeting via CSS selectors, JavaScript, or HTMX.
     * </p>
     *
     * @param id the HTML id attribute value
     * @return this Header for method chaining
     */
    public Header withId(String id) {
        this.id = id;
        if (id != null) {
            this.withAttribute("id", id);
        }
        return this;
    }

    public String getText() {
        return text;
    }

    public HeaderLevel getLevel() {
        return level;
    }

    public Header withClass(String className) {
        this.addClass(className);
        return this;
    }

    /**
     * Aligns header text to the left (default).
     */
    public Header left() {
        this.alignment = Alignment.LEFT.getCssClass();
        updateAlignmentClass();
        return this;
    }

    /**
     * Aligns header text to the center.
     */
    public Header center() {
        this.alignment = Alignment.CENTER.getCssClass();
        updateAlignmentClass();
        return this;
    }

    /**
     * Aligns header text to the right.
     */
    public Header right() {
        this.alignment = Alignment.RIGHT.getCssClass();
        updateAlignmentClass();
        return this;
    }

    /**
     * Justifies header text.
     */
    public Header justify() {
        this.alignment = Alignment.JUSTIFY.getCssClass();
        updateAlignmentClass();
        return this;
    }

    private void updateAlignmentClass() {
        if (alignment == null) {
            return;
        }

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

    /**
     * Adds a default horizontal divider above the header.
     */
    public Header withTopBar() {
        this.topBar = Divider.horizontal();
        return this;
    }

    /**
     * Adds a custom divider above the header.
     *
     * @param divider the divider component to use
     */
    public Header withTopBar(Component divider) {
        this.topBar = divider;
        return this;
    }

    /**
     * Adds a default horizontal divider below the header.
     */
    public Header withBottomBar() {
        this.bottomBar = Divider.horizontal();
        return this;
    }

    /**
     * Adds a custom divider below the header.
     *
     * @param divider the divider component to use
     */
    public Header withBottomBar(Component divider) {
        this.bottomBar = divider;
        return this;
    }

    @Override
    public String render(RenderContext context) {
        // If no bars, render normally
        if (topBar == null && bottomBar == null) {
            return super.render(context);
        }

        // Wrap with bars
        HtmlTag wrapper = new HtmlTag("div").withAttribute("class", "header-wrapper");
        if (topBar != null) {
            wrapper.withChild(topBar);
        }

        // Add this header as a child that renders itself using super.render()
        wrapper.withChild(new Component() {
            @Override
            public String render(RenderContext ctx) {
                return Header.super.render(ctx);
            }
            @Override
            public String render() {
                return render(RenderContext.empty());
            }
        });

        if (bottomBar != null) {
            wrapper.withChild(bottomBar);
        }

        return wrapper.render(context);
    }

    @Override
    public String render() {
        return render(RenderContext.empty());
    }

}
