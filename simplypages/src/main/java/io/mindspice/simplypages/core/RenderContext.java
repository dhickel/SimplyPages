package io.mindspice.simplypages.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A container for per-request values used to fill {@link SlotKey}s during rendering.
 * This class is immutable.
 */
public class RenderContext {
    private static final RenderContext EMPTY = new RenderContext(Collections.emptyMap());
    private final Map<SlotKey<?>, Object> values;

    private RenderContext(Map<SlotKey<?>, Object> values) {
        this.values = values;
    }

    /**
     * Returns an empty context.
     */
    public static RenderContext empty() {
        return EMPTY;
    }

    /**
     * Creates a new builder for constructing a context.
     */
    public static RenderContextBuilder builder() {
        return new RenderContextBuilder();
    }

    /**
     * Creates a context from a map of keys to values.
     */
    public static RenderContext of(Map<SlotKey<?>, Object> values) {
        return new RenderContext(new HashMap<>(values));
    }

    /**
     * Retrieves the value associated with the given key.
     * If the key is not present, returns the key's default value (if any).
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(SlotKey<T> key) {
        Object value = values.get(key);
        if (value != null) {
            return Optional.of((T) value);
        }
        return Optional.ofNullable(key.getDefault(this));
    }

    public static class RenderContextBuilder {
        private final Map<SlotKey<?>, Object> map = new HashMap<>();

        public <T> RenderContextBuilder with(SlotKey<T> key, T value) {
            map.put(key, value);
            return this;
        }

        public RenderContext build() {
            return new RenderContext(map);
        }
    }
}
