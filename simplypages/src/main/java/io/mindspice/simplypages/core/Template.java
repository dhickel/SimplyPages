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
            Optional<SlotEntry> entryOpt = context.getEntry(key);
            if (entryOpt.isPresent()) {
                SlotEntry entry = entryOpt.get();
                switch (entry) {
                    case SlotEntry.CompiledEntry compiled -> sb.append(compiled.html());
                    case SlotEntry.LiveEntry live -> {
                        String rendered = renderValue(live.value(), context);
                        if (context.getPolicy() == RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT) {
                            context.putCompiled(key, rendered);
                        }
                        sb.append(rendered);
                    }
                }
                return;
            }

            // Defaults are always live-rendered and never persisted as compiled entries.
            context.get(key).ifPresent(val -> sb.append(renderValue(val, context)));
        }
    }

    private static class TextSlotSegment implements Segment {
        private final SlotKey<String> key;
        TextSlotSegment(SlotKey<String> key) { this.key = key; }
        @Override
        public void render(RenderContext context, StringBuilder sb) {
            Optional<SlotEntry> entryOpt = context.getEntry(key);
            if (entryOpt.isPresent()) {
                SlotEntry entry = entryOpt.get();
                switch (entry) {
                    case SlotEntry.CompiledEntry compiled -> sb.append(compiled.html());
                    case SlotEntry.LiveEntry live -> {
                        String rendered = live.value() == null ? "" : Encode.forHtml(live.value().toString());
                        if (context.getPolicy() == RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT) {
                            context.putCompiled(key, rendered);
                        }
                        sb.append(rendered);
                    }
                }
                return;
            }

            // Defaults are always live-rendered and never persisted as compiled entries.
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
        switch (component) {
            case Module module -> {
                module.build();
                compileModuleAsTag(module);
            }
            case Slot<?> slot -> segments.add(new SlotSegment(slot.getKey()));
            case HtmlTag tag -> compileTag(tag);
            default -> segments.add(new ComponentSegment(component));
        }
    }

    private void compileModuleAsTag(Module module) {
        compileTag(module);
    }

    private void compileTag(HtmlTag tag) {
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

    private static String renderValue(Object val, RenderContext context) {
        if (val == null) {
            return "";
        }
        if (val instanceof Component component) {
            return component.render(context);
        }
        return Encode.forHtml(val.toString());
    }
}
