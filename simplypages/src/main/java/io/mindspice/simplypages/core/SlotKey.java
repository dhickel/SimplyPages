package io.mindspice.simplypages.core;

import java.util.function.Function;

/**
 * A typed key for accessing values in a {@link RenderContext}.
 *
 * @param <T> the type of value associated with this key
 */
public record SlotKey<T>(String name, Function<RenderContext, T> defaultProvider) {

    /**
     * Creates a key with the given name and no default value.
     */
    public static <T> SlotKey<T> of(String name) {
        return new SlotKey<>(name, null);
    }

    /**
     * Creates a key with the given name and a constant default value.
     */
    public static <T> SlotKey<T> of(String name, T defaultValue) {
        return new SlotKey<>(name, ctx -> defaultValue);
    }

    /**
     * Creates a key with the given name and a dynamic default value provider.
     */
    public static <T> SlotKey<T> of(String name, Function<RenderContext, T> defaultProvider) {
        return new SlotKey<>(name, defaultProvider);
    }

    /**
     * Returns the default value for this key, if any.
     */
    public T getDefault(RenderContext context) {
        return defaultProvider != null ? defaultProvider.apply(context) : null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SlotKey<?> other && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "SlotKey{" + name + "}";
    }
}
