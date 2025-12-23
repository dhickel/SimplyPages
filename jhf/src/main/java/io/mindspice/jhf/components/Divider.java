package io.mindspice.jhf.components;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;
import io.mindspice.jhf.core.RenderContext;

import java.util.stream.Stream;

/**
 * Divider component for creating horizontal or vertical visual separators.
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

    public static Divider horizontal() {
        return new Divider(DividerOrientation.HORIZONTAL);
    }

    public static Divider vertical() {
        return new Divider(DividerOrientation.VERTICAL);
    }

    public Divider thin() {
        this.thickness = DividerThickness.THIN;
        return this;
    }

    public Divider medium() {
        this.thickness = DividerThickness.MEDIUM;
        return this;
    }

    public Divider thick() {
        this.thickness = DividerThickness.THICK;
        return this;
    }

    public Divider solid() {
        this.style = DividerStyle.SOLID;
        return this;
    }

    public Divider dashed() {
        this.style = DividerStyle.DASHED;
        return this;
    }

    public Divider dotted() {
        this.style = DividerStyle.DOTTED;
        return this;
    }

    public Divider withColor(String color) {
        this.color = color;
        return this;
    }

    public Divider withText(String text) {
        this.text = text;
        return this;
    }

    public Divider withHeight(String height) {
        if (orientation == DividerOrientation.VERTICAL) {
            this.addStyle("height", height);
        }
        return this;
    }

    @Override
    public Divider withClass(String className) {
        super.addClass(className);
        return this;
    }

    @Override
    protected Stream<Component> getChildrenStream() {
        Stream.Builder<Component> builder = Stream.builder();

        if (text != null && orientation == DividerOrientation.HORIZONTAL) {
             HtmlTag textSpan = new HtmlTag("span")
                .withAttribute("class", "divider-text")
                .withInnerText(text);
             builder.add(textSpan);
        }

        return Stream.concat(builder.build(), super.getChildrenStream());
    }

    // Override render(RenderContext) to apply styles/classes BEFORE invoking super.render
    @Override
    public String render(RenderContext context) {
        // Apply styles and classes based on current state
        this.withAttribute("style", buildStyle());

        if (text != null && orientation == DividerOrientation.HORIZONTAL) {
            // We need to ensure the class includes 'divider-with-text'
            // We use addClass to preserve other classes
             this.addClass("divider-with-text");
        }

        return super.render(context);
    }

    private String buildStyle() {
        StringBuilder styleBuilder = new StringBuilder();

        // Preserve existing height style if any
        // Accessing attributes is hard here without parsing.
        // We will just regenerate the border style.

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
                styleBuilder.append("position: relative; text-align: center; margin: 1rem 0;");
            }
        } else {
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
