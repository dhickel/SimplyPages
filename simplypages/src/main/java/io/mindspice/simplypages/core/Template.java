package io.mindspice.simplypages.core;

import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A reusable template for rendering components with dynamic content.
 * <p>
 * This class optimizes rendering by "compiling" the component tree into a linear sequence of
 * static string segments and dynamic slot lookups.
 * </p>
 */
public class Template {

    private interface Segment {
        void render(RenderContext context, StringBuilder sb);
    }

    private static class StringSegment implements Segment {
        private final String content;
        StringSegment(String content) { this.content = content; }
        @Override public void render(RenderContext context, StringBuilder sb) { sb.append(content); }
    }

    private static class SlotSegment implements Segment {
        private final SlotKey<?> key;
        SlotSegment(SlotKey<?> key) { this.key = key; }

        @Override
        public void render(RenderContext context, StringBuilder sb) {
             Optional<?> valOpt = context.get(key);
             if (valOpt.isPresent()) {
                 Object val = valOpt.get();
                 if (val instanceof Component) {
                     sb.append(((Component) val).render(context));
                 } else {
                     sb.append(Encode.forHtml(val.toString()));
                 }
             }
        }
    }

    private static class TextSlotSegment implements Segment {
        private final SlotKey<String> key;
        TextSlotSegment(SlotKey<String> key) { this.key = key; }
        @Override
        public void render(RenderContext context, StringBuilder sb) {
             context.get(key).ifPresent(val -> sb.append(Encode.forHtml(val)));
        }
    }

    private static class ComponentSegment implements Segment {
        private final Component component;
        ComponentSegment(Component component) { this.component = component; }
        @Override public void render(RenderContext context, StringBuilder sb) { sb.append(component.render(context)); }
    }

    private final List<Segment> segments = new ArrayList<>();

    private Template(Component root) {
        compile(root);
        optimize();
    }

    public static Template of(Component root) {
        return new Template(root);
    }

    private void compile(Component component) {
        if (component instanceof Module) {
            ((Module) component).build();
        }

        if (component instanceof Slot) {
            segments.add(new SlotSegment(((Slot<?>) component).getKey()));
        } else if (component instanceof HtmlTag) {
            HtmlTag tag = (HtmlTag) component;

            StringBuilder sb = new StringBuilder();
            sb.append("<").append(tag.tagName);
            for (Attribute attr : tag.attributes) {
                sb.append(attr.render());
            }
            if (tag.selfClosing) {
                sb.append(" />");
                segments.add(new StringSegment(sb.toString()));
                return;
            }
            sb.append(">");
            segments.add(new StringSegment(sb.toString()));

            if (tag.innerTextSlot != null) {
                segments.add(new TextSlotSegment(tag.innerTextSlot));
            } else if (!tag.innerText.isEmpty()) {
                String text = tag.trustedHtml ? tag.innerText : Encode.forHtml(tag.innerText);
                segments.add(new StringSegment(text));
            }

            for (Component child : tag.children) {
                compile(child);
            }

            segments.add(new StringSegment("</" + tag.tagName + ">"));

        } else {
            // Opaque component
            segments.add(new ComponentSegment(component));
        }
    }

    private void optimize() {
        List<Segment> optimized = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (Segment seg : segments) {
            if (seg instanceof StringSegment) {
                buffer.append(((StringSegment) seg).content);
            } else {
                if (buffer.length() > 0) {
                    optimized.add(new StringSegment(buffer.toString()));
                    buffer.setLength(0);
                }
                optimized.add(seg);
            }
        }
        if (buffer.length() > 0) {
            optimized.add(new StringSegment(buffer.toString()));
        }
        segments.clear();
        segments.addAll(optimized);
    }

    public String render(RenderContext context) {
         StringBuilder sb = new StringBuilder();
         for (Segment segment : segments) {
             segment.render(context, sb);
         }
         return sb.toString();
    }
}
