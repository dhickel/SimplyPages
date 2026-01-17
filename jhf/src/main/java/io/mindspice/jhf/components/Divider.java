package io.mindspice.jhf.components;

import io.mindspice.jhf.core.HtmlTag;

/**
 * Divider component for creating horizontal or vertical visual separators.
 *
 * <p>Dividers are visual elements that separate content sections. They can be
 * styled with different thicknesses, colors, and styles (solid, dashed, dotted).</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Basic horizontal divider (default)
 * Divider.horizontal();
 *
 * // Thick horizontal divider
 * Divider.horizontal().thick();
 *
 * // Dashed divider
 * Divider.horizontal().dashed();
 *
 * // Dotted divider with custom color
 * Divider.horizontal().dotted().withColor("#cccccc");
 *
 * // Vertical divider (useful in flex layouts)
 * Divider.vertical().withHeight("100px");
 *
 * // Divider with text
 * Divider.horizontal().withText("or");
 * }</pre>
 */
public class Divider extends HtmlTag {

    public enum DividerStyle {
        SOLID, DASHED, DOTTED
    }

    public enum DividerThickness {
        THIN("1px"),
        MEDIUM("2px"),
        THICK("4px");

        private final String thickness;

        DividerThickness(String thickness) {
            this.thickness = thickness;
        }

        public String getThickness() {
            return thickness;
        }
    }

    public enum DividerOrientation {
        HORIZONTAL, VERTICAL
    }

    private DividerOrientation orientation;
    private DividerThickness thickness = DividerThickness.THIN;
    private DividerStyle style = DividerStyle.SOLID;
    private String color = null;
    private String text = null;

    private Divider(DividerOrientation orientation) {
        super("div");
        this.orientation = orientation;
        this.withAttribute("class", "divider divider-" + orientation.name().toLowerCase());
    }

    /**
     * Creates a horizontal divider (default).
     */
    public static Divider horizontal() {
        return new Divider(DividerOrientation.HORIZONTAL);
    }

    /**
     * Creates a vertical divider.
     */
    public static Divider vertical() {
        return new Divider(DividerOrientation.VERTICAL);
    }

    /**
     * Sets thin thickness (1px) - default.
     */
    public Divider thin() {
        this.thickness = DividerThickness.THIN;
        return this;
    }

    /**
     * Sets medium thickness (2px).
     */
    public Divider medium() {
        this.thickness = DividerThickness.MEDIUM;
        return this;
    }

    /**
     * Sets thick thickness (4px).
     */
    public Divider thick() {
        this.thickness = DividerThickness.THICK;
        return this;
    }

    /**
     * Sets solid line style (default).
     */
    public Divider solid() {
        this.style = DividerStyle.SOLID;
        return this;
    }

    /**
     * Sets dashed line style.
     */
    public Divider dashed() {
        this.style = DividerStyle.DASHED;
        return this;
    }

    /**
     * Sets dotted line style.
     */
    public Divider dotted() {
        this.style = DividerStyle.DOTTED;
        return this;
    }

    /**
     * Sets the color of the divider.
     *
     * @param color CSS color value (e.g., "#cccccc", "rgb(200,200,200)", "gray")
     */
    public Divider withColor(String color) {
        this.color = color;
        return this;
    }

    /**
     * Sets text to display in the middle of a horizontal divider.
     *
     * @param text the text to display
     */
    public Divider withText(String text) {
        this.text = text;
        return this;
    }

    /**
     * Sets a custom height for vertical dividers.
     *
     * @param height CSS height value (e.g., "100px", "50%")
     */
    public Divider withHeight(String height) {
        if (orientation == DividerOrientation.VERTICAL) {
            this.withAttribute("style", buildStyle() + " height: " + height + ";");
        }
        return this;
    }

    public Divider withClass(String className) {
        String currentClass = "divider divider-" + orientation.name().toLowerCase();
        this.withAttribute("class", currentClass + " " + className);
        return this;
    }

    @Override
    public String render() {
        // Apply styling
        this.withAttribute("style", buildStyle());

        // If there's text, create a special structure for centered text
        if (text != null && orientation == DividerOrientation.HORIZONTAL) {
            this.withAttribute("class", "divider divider-horizontal divider-with-text");
            HtmlTag textSpan = new HtmlTag("span")
                .withAttribute("class", "divider-text")
                .withInnerText(text);
            super.withChild(textSpan);
        }

        return super.render();
    }

    private String buildStyle() {
        StringBuilder styleBuilder = new StringBuilder();

        if (orientation == DividerOrientation.HORIZONTAL) {
            if (text == null) {
                styleBuilder.append("border-top: ")
                    .append(thickness.getThickness())
                    .append(" ")
                    .append(style.name().toLowerCase())
                    .append(" ");

                if (color != null) {
                    styleBuilder.append(color);
                } else {
                    styleBuilder.append("var(--border-color, #e0e0e0)");
                }
                styleBuilder.append("; width: 100%; display: block; line-height: 0;");
            } else {
                // Style for divider with text is handled by CSS
                styleBuilder.append("position: relative; text-align: center; margin: 1rem 0;");
            }
        } else {
            // Vertical divider
            styleBuilder.append("border-left: ")
                .append(thickness.getThickness())
                .append(" ")
                .append(style.name().toLowerCase())
                .append(" ");

            if (color != null) {
                styleBuilder.append(color);
            } else {
                styleBuilder.append("var(--border-color, #e0e0e0)");
            }
            styleBuilder.append("; display: inline-block;");
        }

        return styleBuilder.toString();
    }
}
