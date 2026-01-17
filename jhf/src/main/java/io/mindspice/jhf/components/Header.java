package io.mindspice.jhf.components;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Header component for text headings with alignment and decoration options.
 *
 * <p>Headers support H1-H6 levels, alignment, and optional top/bottom dividers.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Basic header
 * Header.H1("Welcome");
 *
 * // Centered header
 * Header.H2("About Us").center();
 *
 * // Header with top divider
 * Header.H3("Section Title").withTopBar();
 *
 * // Header with both dividers
 * Header.H2("Important").withTopBar().withBottomBar();
 *
 * // Header with custom divider styling
 * Header.H1("Main Title")
 *     .center()
 *     .withTopBar(Divider.horizontal().thick())
 *     .withBottomBar(Divider.horizontal().dashed());
 * }</pre>
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

    private Component topBar;
    private Component bottomBar;
    private String alignment;

    public Header(HeaderLevel level, String text) {
        super(level.name().toLowerCase());
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

    public Header withClass(String className) {
        this.withAttribute("class", className);
        return this;
    }

    /**
     * Aligns header text to the left (default).
     */
    public Header left() {
        this.alignment = Alignment.LEFT.getCssClass();
        return this;
    }

    /**
     * Aligns header text to the center.
     */
    public Header center() {
        this.alignment = Alignment.CENTER.getCssClass();
        return this;
    }

    /**
     * Aligns header text to the right.
     */
    public Header right() {
        this.alignment = Alignment.RIGHT.getCssClass();
        return this;
    }

    /**
     * Justifies header text.
     */
    public Header justify() {
        this.alignment = Alignment.JUSTIFY.getCssClass();
        return this;
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
    public String render() {
        // Apply alignment if set
        if (alignment != null) {
            // Check if class attribute exists
            boolean hasClass = attributes.stream()
                .anyMatch(attr -> "class".equals(attr.getName()));

            if (hasClass) {
                // Append to existing class
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

        // If no bars, render normally
        if (topBar == null && bottomBar == null) {
            return super.render();
        }

        // Wrap with bars
        HtmlTag wrapper = new HtmlTag("div").withAttribute("class", "header-wrapper");
        if (topBar != null) {
            wrapper.withChild(topBar);
        }

        // Clone this header's HTML as a child
        wrapper.withChild(new Component() {
            @Override
            public String render() {
                return Header.super.render();
            }
        });

        if (bottomBar != null) {
            wrapper.withChild(bottomBar);
        }

        return wrapper.render();
    }
}

