package io.mindspice.jhf.core;

import java.util.Objects;
import java.util.function.Function;

/**
 * A typed key for accessing values in a {@link RenderContext}.
 *
 * @param <T> the type of value associated with this key
 */
public class SlotKey<T> {
    private final String name;
    private final Function<RenderContext, T> defaultProvider;

    private SlotKey(String name, Function<RenderContext, T> defaultProvider) {
        this.name = name;
        this.defaultProvider = defaultProvider;
    }

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

    public String getName() {
        return name;
    }

    /**
     * Returns the default value for this key, if any.
     */
    public T getDefault(RenderContext context) {
        return defaultProvider != null ? defaultProvider.apply(context) : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SlotKey<?> slotKey = (SlotKey<?>) o;
        return Objects.equals(name, slotKey.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "SlotKey{" + name + "}";
    }
}
