package io.mindspice.jhf.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Context holding the data for rendering a Template.
 * Maps SlotKeys to their content.
 */
public class RenderContext {
    private final Map<SlotKey<?>, Object> data = new HashMap<>();

    private RenderContext() {}

    /**
     * Creates a new empty RenderContext.
     * @return A new RenderContext
     */
    public static RenderContext create() {
        return new RenderContext();
    }

    /**
     * Sets the value for a specific SlotKey.
     *
     * @param key The slot key
     * @param value The value to fill the slot with
     * @param <T> The type of the value
     * @return This RenderContext for chaining
     */
    public <T> RenderContext set(SlotKey<T> key, T value) {
        data.put(key, value);
        return this;
    }

    /**
     * Gets the value for a SlotKey.
     *
     * @param key The slot key
     * @param <T> The type of the value
     * @return The value, or null if not set
     */
    @SuppressWarnings("unchecked")
    public <T> T get(SlotKey<T> key) {
        return (T) data.get(key);
    }
}
