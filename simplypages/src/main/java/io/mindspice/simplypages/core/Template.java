package io.mindspice.simplypages.core;

import org.owasp.encoder.Encode;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Compiled render plan built from a component tree.
 *
 * <p>Compilation flattens static HTML into string segments and keeps dynamic portions as slot or
 * component segments. Modules are built during compilation to honor build-once lifecycle rules.</p>
 *
 * <p>Security boundary: text slots and non-component values are escaped; compiled slot entries are
 * treated as trusted HTML and inserted as-is.</p>
 *
 * <p>Mutability/thread-safety: a template is effectively immutable after construction and can be
 * reused across requests. Thread-safety depends on render contexts passed at call sites.</p>
 */
public class Template {

    /**
     * Unit of compiled output capable of appending itself during render.
     */
    private interface Segment {
        void render(RenderContext context, StringBuilder sb);
    }

    /**
     * Static literal HTML segment.
     */
    private static class StringSegment implements Segment {
        private final String content;
        StringSegment(String content) { this.content = content; }
        @Override public void render(RenderContext context, StringBuilder sb) { sb.append(content); }
    }

    /**
     * Dynamic segment that resolves a slot as component-or-escaped-value.
     */
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

            context.get(key).ifPresent(val -> sb.append(renderValue(val, context)));
        }
    }

    /**
     * Dynamic segment for escaped text slots used by {@link HtmlTag#innerTextSlot}.
     */
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

            context.get(key).ifPresent(val -> sb.append(Encode.forHtml(val)));
        }
    }

    /**
     * Dynamic segment that delegates rendering to a component at render time.
     */
    private static class ComponentSegment implements Segment {
        private final Component component;
        ComponentSegment(Component component) { this.component = component; }
        @Override public void render(RenderContext context, StringBuilder sb) { sb.append(component.render(context)); }
    }

    /** Compiled segments in render order. */
    private final List<Segment> segments = new ArrayList<>();

    /**
     * Compiles and optimizes a template from {@code root}.
     */
    private Template(Component root) {
        compile(root);
        optimize();
    }

    /**
     * Compiles a reusable template from a component root.
     */
    public static Template of(Component root) {
        return new Template(root);
    }

    /**
     * Compiles a component subtree into render segments.
     */
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

    /**
     * Compiles a module as an already-built tag subtree.
     */
    private void compileModuleAsTag(Module module) {
        compileTag(module);
    }

    /**
     * Compiles an {@link HtmlTag} including static shell, text policy, and children.
     */
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

    /**
     * Coalesces adjacent {@link StringSegment}s to reduce render overhead.
     */
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

    /**
     * Renders this template with the provided context.
     */
    public String render(RenderContext context) {
         StringBuilder sb = new StringBuilder();
         for (Segment segment : segments) {
             segment.render(context, sb);
         }
         return sb.toString();
    }

    /**
     * Renders arbitrary slot values with component-aware dispatch.
     */
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
