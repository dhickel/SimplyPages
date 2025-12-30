package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;

/**
 * Spacer component for adding vertical or horizontal spacing between elements.
 *
 * <p>Spacers are empty divs with configurable height or width for creating
 * visual separation between components.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Small vertical spacing (16px)
 * Spacer.vertical().small();
 *
 * // Medium vertical spacing (32px) - default
 * Spacer.vertical().medium();
 *
 * // Large vertical spacing (48px)
 * Spacer.vertical().large();
 *
 * // Extra large vertical spacing (64px)
 * Spacer.vertical().extraLarge();
 *
 * // Custom vertical spacing
 * Spacer.vertical().custom("100px");
 *
 * // Horizontal spacer with medium width
 * Spacer.horizontal().medium();
 * }</pre>
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
