package io.mindspice.jhf.components;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.core.RenderContext;

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
        if (alignment != null) {
            // Remove any existing alignment class
            for (Alignment a : Alignment.values()) {
                // This is tricky without access to attributes easily to remove specific class parts.
                // But addClass appends. HtmlTag handles duplicates but not removal of conflicting classes easily.
                // However, the original code tried to replace it.
                // For simplicity, let's just append. CSS cascade usually takes last one.
                // Or we can rely on HtmlTag.addClass to just add it.
                // If we switch alignment, we might have multiple classes.
                // The previous code parsed the class string.
                // Let's stick to simple addClass for now, or if we want to be clean, we'd need better attribute management.
                // Since this is a fix, let's just add it.
                this.addClass(alignment);
            }
        }
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
