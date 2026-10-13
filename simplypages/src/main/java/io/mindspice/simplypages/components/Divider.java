package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Visual separator supporting horizontal/vertical orientation and style options.
 *
 * <p>Rendering derives temporary attributes without changing stored configuration. Horizontal
 * dividers reserve the exact lowercase properties {@code border-top}, {@code width},
 * {@code display}, {@code line-height}, {@code position}, {@code text-align}, {@code margin}
 * and the whole outer class token {@code divider-with-text}. With null text, the first four
 * properties are generated and the last three/token suppressed; with any non-null text,
 * including empty text, the reverse applies. Inactive reserved names are suppressed even when
 * caller-configured. Vertical dividers generate only {@code border-left} and {@code display};
 * they do not suppress other properties or the outer text token.</p>
 *
 * <p>Other declarations, case variants and classes remain caller-owned. Cleanup does not repair
 * arbitrary CSS conflicts. Configuration is mutable and must finish before sharing; templates
 * retain this renderer live. Child delegates and request contexts have separate sharing obligations.</p>
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
        if (color != null) new HtmlTag("span").addStyle("color", color);
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

    @Override
    public String render(RenderContext context) {
        RenderTag output = new RenderTag();
        if (orientation == DividerOrientation.HORIZONTAL) {
            output.suppressStyles(text == null
                    ? new String[]{"position", "text-align", "margin"}
                    : new String[]{"border-top", "width", "display", "line-height"});
            if (text == null) {
                output.suppressTextClass();
            }
        }
        for (String declaration : buildStyle().split(";")) {
            int colon = declaration.indexOf(':');
            if (colon >= 0) {
                output.addStyle(declaration.substring(0, colon).trim(), declaration.substring(colon + 1).trim());
            }
        }
        if (text != null && orientation == DividerOrientation.HORIZONTAL) {
            output.addClass("divider-with-text");
        }
        return output.render(context);
    }

    private final class RenderTag extends HtmlTag {
        private RenderTag() {
            super(Divider.this.tagName, Divider.this.selfClosing);
            attributes.addAll(Divider.this.attributes);
            id = Divider.this.id;
            innerText = Divider.this.innerText;
            innerTextSlot = Divider.this.innerTextSlot;
            trustedHtml = Divider.this.trustedHtml;
        }

        @Override
        protected Stream<Component> getChildrenStream() {
            return Divider.this.getChildrenStream();
        }

        private void suppressStyles(String[] properties) {
            attributes.stream().filter(attr -> "style".equalsIgnoreCase(attr.name())).findFirst()
                    .ifPresent(attr -> {
                        String styles = attr.value() == null ? "" : attr.value();
                        for (String property : properties) {
                            styles = styles.replaceAll("(^|;)\\s*" + Pattern.quote(property) + "\\s*:[^;]*", "$1");
                        }
                        withAttribute(attr.name(), styles);
                    });
        }

        private void suppressTextClass() {
            attributes.stream().filter(attr -> "class".equalsIgnoreCase(attr.name())).findFirst()
                    .filter(attr -> attr.value() != null)
                    .ifPresent(attr -> {
                        String[] tokens = attr.value().trim().split("\\s+");
                        if (Arrays.asList(tokens).contains("divider-with-text")) {
                            withAttribute(attr.name(), Arrays.stream(tokens)
                                    .filter(token -> !"divider-with-text".equals(token))
                                    .collect(Collectors.joining(" ")));
                        }
                    });
        }
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
