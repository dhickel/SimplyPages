package io.mindspice.simplypages.core;

import java.util.Map;

/**
 * Contract for any renderable SimplyPages UI node.
 *
 * <p>Lifecycle: callers may render with an explicit {@link RenderContext} or use
 * {@link #render()} to render against {@link RenderContext#empty()}.</p>
 *
 * <p>Usage boundary: slot-aware rendering works directly through this interface; {@link Template}
 * is the optional compiled wrapper for reuse of stable render structures.</p>
 *
 * <p>Mutability/thread-safety: this interface does not impose either. Implementations must
 * document whether instances are mutable and safe to share across threads or requests.</p>
 */
public interface Component {
    /**
     * Renders this component using the supplied render context.
     *
     * @param context context used for slot resolution and render policy
     * @return rendered HTML for this component
     */
    String render(RenderContext context);

    /**
     * Renders this component with an empty context.
     *
     * @return rendered HTML for this component
     */
    default String render() {
        return render(RenderContext.empty());
    }

    /**
     * Renders this component from raw slot values.
     *
     * @param values raw slot values keyed by {@link SlotKey}
     * @return rendered HTML for this component
     */
    default String render(Map<? extends SlotKey<?>, ?> values) {
        return render(RenderContext.of(values));
    }

    /**
     * Compiles this component into a reusable {@link Template}.
     *
     * @return compiled template
     */
    default Template compile() {
        return Template.of(this);
    }
}
