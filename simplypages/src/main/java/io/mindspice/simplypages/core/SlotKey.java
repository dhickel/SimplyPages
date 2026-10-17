package io.mindspice.simplypages.core;

import java.util.Objects;
import java.util.function.Function;

/**
 * Typed identifier for values stored in a {@link RenderContext}.
 *
 * <p>Names must be non-null and are preserved literally, including empty and blank strings.
 * No trimming, case folding, or Unicode normalization is applied.</p>
 *
 * <p>Equality and hash code use only {@link #name()}, not the generic type or default provider.
 * Separately created keys with the same name address the same slot; use a consistent type for
 * each name.</p>
 *
 * <p>Usage boundary: keys are consumed by slot-aware rendering in either direct component renders
 * or compiled {@link Template} renders.</p>
 *
 * <p>Mutability/thread-safety: immutable and thread-safe. Thread-safety of default evaluation
 * depends on the provided {@link #defaultProvider()} implementation.</p>
 *
 * @param <T> slot value type
 * @param name non-null literal slot name
 * @param defaultProvider nullable default function; not evaluated during construction
 */
public record SlotKey<T>(String name, Function<RenderContext, T> defaultProvider) {

    /**
     * Creates a key without evaluating its default provider.
     *
     * @param name non-null literal slot name
     * @param defaultProvider default function, or {@code null} for no default
     * @throws NullPointerException if {@code name} is null
     */
    public SlotKey {
        name = Objects.requireNonNull(name, "name");
    }

    /**
     * Creates a key with no default provider.
     *
     * @param name non-null literal slot name
     * @throws NullPointerException if {@code name} is null
     */
    public static <T> SlotKey<T> of(String name) {
        return new SlotKey<>(name, null);
    }

    /**
     * Creates a key with a constant default value.
     *
     * @param name non-null literal slot name
     * @param defaultValue default used when slot has no explicit entry
     * @throws NullPointerException if {@code name} is null
     */
    public static <T> SlotKey<T> of(String name, T defaultValue) {
        return new SlotKey<>(name, ctx -> defaultValue);
    }

    /**
     * Creates a key with a computed default.
     *
     * @param name non-null literal slot name
     * @param defaultProvider default function invoked when slot has no explicit entry, or {@code null}
     *                        for no default; not evaluated during construction
     * @throws NullPointerException if {@code name} is null
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
