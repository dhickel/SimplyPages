package io.mindspice.jhf.modules;

import io.mindspice.jhf.core.Component;

/**
 * Represents a dynamic content update for a {@link DynamicModule}.
 *
 * <p>Pairs a tag name (defined in the module's template) with a
 * component that should replace the placeholder at that tag.</p>
 */
public record DynamicContent(String tag, Component component) {
    /**
     * Creates a new DynamicContent pair.
     *
     * @param tag the tag name identifying the placeholder
     * @param component the component to render in place of the placeholder
     * @return a new DynamicContent instance
     */
    public static DynamicContent of(String tag, Component component) {
        return new DynamicContent(tag, component);
    }
}
