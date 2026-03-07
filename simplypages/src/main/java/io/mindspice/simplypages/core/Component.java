package io.mindspice.simplypages.core;

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
    default String render(RenderContext context) {
        return render();
    }

    /**
     * Renders this component with an empty context.
     *
     * @return rendered HTML for this component
     */
    default String render() {
        return render(RenderContext.empty());
    }
}
