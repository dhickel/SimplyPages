package io.mindspice.simplypages.core;

import java.util.function.Function;

/**
 * Typed identifier for values stored in a {@link RenderContext}.
 *
 * <p>Equality and hash code use only {@link #name()} so separately created keys with the same
 * name address the same slot.</p>
 *
 * <p>Usage boundary: keys are consumed by slot-aware rendering in either direct component renders
 * or compiled {@link Template} renders.</p>
 *
 * <p>Mutability/thread-safety: immutable and thread-safe. Thread-safety of default evaluation
 * depends on the provided {@link #defaultProvider()} implementation.</p>
 *
 * @param <T> slot value type
 */
public record SlotKey<T>(String name, Function<RenderContext, T> defaultProvider) {

    /**
     * Creates a key with no default provider.
     *
     * @param name stable slot name
     */
    public static <T> SlotKey<T> of(String name) {
        return new SlotKey<>(name, null);
    }

    /**
     * Creates a key with a constant default value.
     *
     * @param name stable slot name
     * @param defaultValue default used when slot has no explicit entry
     */
    public static <T> SlotKey<T> of(String name, T defaultValue) {
        return new SlotKey<>(name, ctx -> defaultValue);
    }

    /**
     * Creates a key with a computed default.
     *
     * @param name stable slot name
     * @param defaultProvider default function invoked when slot has no explicit entry
     */
    public static <T> SlotKey<T> of(String name, Function<RenderContext, T> defaultProvider) {
        return new SlotKey<>(name, defaultProvider);
    }

    /**
     * Resolves the default value for this key.
     *
     * @param context render context passed to {@link #defaultProvider()}
     * @return default value, or {@code null} when no provider exists
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
