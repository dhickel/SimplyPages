package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Empty spacing helper for vertical or horizontal gaps.
 *
 * <p>Mutable and not thread-safe. Orientation and size styles are applied to the same instance. Mutate within a request-scoped flow. For reuse, stop mutating and render as a stable structure with per-request slot/context values.</p>
 */
public class Spacer extends HtmlTag {

    public enum SpacerSize {
        SMALL("16px"),
        MEDIUM("32px"),
        LARGE("48px"),
        EXTRA_LARGE("64px");

        private final String size;

        SpacerSize(String size) {
            this.size = size;
        }

        public String getSize() {
            return size;
        }
    }

    public enum SpacerType {
        VERTICAL, HORIZONTAL
    }

    private SpacerType type;

    private Spacer(SpacerType type) {
        super("div");
        this.type = type;
        this.withAttribute("class", "spacer spacer-" + type.name().toLowerCase());
    }

    /**
     * Creates a vertical spacer (adds height).
     */
    public static Spacer vertical() {
        return new Spacer(SpacerType.VERTICAL);
    }

    /**
     * Creates a horizontal spacer (adds width).
     */
    public static Spacer horizontal() {
        return new Spacer(SpacerType.HORIZONTAL);
    }

    /**
     * Sets small spacing (16px).
     */
    public Spacer small() {
        return setSize(SpacerSize.SMALL);
    }

    /**
     * Sets medium spacing (32px).
     */
    public Spacer medium() {
        return setSize(SpacerSize.MEDIUM);
    }

    /**
     * Sets large spacing (48px).
     */
    public Spacer large() {
        return setSize(SpacerSize.LARGE);
    }

    /**
     * Sets extra large spacing (64px).
     */
    public Spacer extraLarge() {
        return setSize(SpacerSize.EXTRA_LARGE);
    }

    /**
     * Sets custom spacing with any CSS size value.
     *
     * @param size CSS size value (e.g., "50px", "3rem", "10%")
     */
    public Spacer custom(String size) {
        String property = type == SpacerType.VERTICAL ? "height" : "width";
        this.withAttribute("style", property + ": " + size + ";");
        return this;
    }

    private Spacer setSize(SpacerSize size) {
        String property = type == SpacerType.VERTICAL ? "height" : "width";
        this.withAttribute("style", property + ": " + size.getSize() + ";");
        return this;
    }

    public Spacer withClass(String className) {
        String currentClass = "spacer spacer-" + type.name().toLowerCase();
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }
}
