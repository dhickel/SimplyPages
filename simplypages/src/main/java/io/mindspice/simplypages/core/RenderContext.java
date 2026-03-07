package io.mindspice.simplypages.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable per-render value container for {@link SlotKey} lookups.
 *
 * <p>Lifecycle: create per request/render flow, populate slot values, render components, then
 * discard. This type is intentionally mutable to support incremental population and optional
 * compile-on-first-hit slot caching.</p>
 *
 * <p>Mutability/thread-safety: mutable and not thread-safe. Use per request by default. Context
 * reuse is valid only when access is confined (no concurrent mutation), especially when using
 * {@link RenderPolicy#COMPILE_ON_FIRST_HIT}.</p>
 */
public class RenderContext {
    /**
     * Controls whether slot render output may be cached as compiled HTML entries.
     */
    public enum RenderPolicy {
        /** Always render live values; never persist compiled slot output. */
        NEVER_COMPILE,
        /** On first live render, store generated HTML as a compiled slot entry. */
        COMPILE_ON_FIRST_HIT
    }

    /** Mutable slot storage keyed by logical slot identity. */
    private final Map<SlotKey<?>, SlotEntry> values;
    /** Active render policy for this context. */
    private RenderPolicy policy;

    /**
     * Creates a context from explicit slot entries and policy.
     */
    private RenderContext(Map<SlotKey<?>, SlotEntry> values, RenderPolicy policy) {
        this.values = values;
        this.policy = policy;
    }

    /**
     * Returns an empty context using {@link RenderPolicy#NEVER_COMPILE}.
     */
    public static RenderContext empty() {
        return new RenderContext(new HashMap<>(), RenderPolicy.NEVER_COMPILE);
    }

    /**
     * Returns a builder for explicit context construction.
     */
    public static RenderContextBuilder builder() {
        return new RenderContextBuilder();
    }

    /**
     * Creates a context from raw slot values with default policy.
     *
     * <p>{@code null} values remove existing entries.</p>
     */
    public static RenderContext of(Map<SlotKey<?>, Object> values) {
        RenderContext context = empty();
        values.forEach((key, value) -> context.putUnchecked(key, value));
        return context;
    }

    /**
     * Resolves the value for {@code key}.
     *
     * <p>Resolution order:</p>
     * <p>1. live entry value</p>
     * <p>2. key default provider (compiled entries intentionally skip cached HTML and fall back to default)</p>
     * <p>3. empty</p>
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(SlotKey<T> key) {
        SlotEntry entry = values.get(key);
        return switch (entry) {
            case SlotEntry.LiveEntry live -> Optional.ofNullable((T) live.value());
            case SlotEntry.CompiledEntry ignored -> Optional.ofNullable(key.getDefault(this));
            case null -> Optional.ofNullable(key.getDefault(this));
        };
    }

    /**
     * Stores a live value for {@code key}; {@code null} removes the entry.
     */
    public <T> RenderContext put(SlotKey<T> key, T value) {
        putUnchecked(key, value);
        return this;
    }

    /**
     * Stores pre-rendered trusted HTML for {@code key}; {@code null} removes the entry.
     */
    public RenderContext putCompiled(SlotKey<?> key, String html) {
        if (html == null) {
            values.remove(key);
            return this;
        }
        values.put(key, new SlotEntry.CompiledEntry(html));
        return this;
    }

    /**
     * Removes any entry for {@code key}.
     */
    public RenderContext remove(SlotKey<?> key) {
        values.remove(key);
        return this;
    }

    /**
     * Removes all entries from this context.
     */
    public RenderContext clear() {
        values.clear();
        return this;
    }

    /**
     * Sets render policy for subsequent template slot handling.
     *
     * @throws NullPointerException when {@code policy} is null
     */
    public RenderContext withPolicy(RenderPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy cannot be null");
        return this;
    }

    /**
     * Returns current render policy.
     */
    public RenderPolicy getPolicy() {
        return policy;
    }

    /**
     * Returns whether {@code key} currently maps to a compiled entry.
     */
    public boolean isCompiled(SlotKey<?> key) {
        return values.get(key) instanceof SlotEntry.CompiledEntry;
    }

    /**
     * Returns compiled HTML for {@code key} when a compiled entry exists.
     */
    public Optional<String> getCompiled(SlotKey<?> key) {
        SlotEntry entry = values.get(key);
        return switch (entry) {
            case SlotEntry.CompiledEntry compiled -> Optional.ofNullable(compiled.html());
            case SlotEntry.LiveEntry ignored -> Optional.empty();
            case null -> Optional.empty();
        };
    }

    /**
     * Returns an immutable snapshot of stored entries.
     */
    public Map<SlotKey<?>, SlotEntry> getEntries() {
        return Map.copyOf(values);
    }

    /**
     * Internal lookup of raw slot entry, including compiled entries.
     */
    Optional<SlotEntry> getEntry(SlotKey<?> key) {
        return Optional.ofNullable(values.get(key));
    }

    /**
     * Stores a live entry without compile-time generic checks; used by bridges/builders.
     */
    private <T> void putUnchecked(SlotKey<T> key, Object value) {
        if (value == null) {
            values.remove(key);
            return;
        }
        values.put(key, new SlotEntry.LiveEntry(value.getClass(), value));
    }

    /**
     * Mutable builder for {@link RenderContext}.
     *
     * <p>Mutability/thread-safety: builder is mutable and not thread-safe. Use within a single
     * request/render composition flow and do not share across concurrent threads.</p>
     */
    public static class RenderContextBuilder {
        /** Builder-local slot entries. */
        private final Map<SlotKey<?>, SlotEntry> map = new HashMap<>();
        /** Builder-local policy value. */
        private RenderPolicy policy = RenderPolicy.NEVER_COMPILE;

        /**
         * Stores a live value for {@code key}; {@code null} removes the entry.
         */
        public <T> RenderContextBuilder with(SlotKey<T> key, T value) {
            if (value == null) {
                map.remove(key);
            } else {
                map.put(key, new SlotEntry.LiveEntry(value.getClass(), value));
            }
            return this;
        }

        /**
         * Sets policy for the built context.
         *
         * @throws NullPointerException when {@code policy} is null
         */
        public RenderContextBuilder withPolicy(RenderPolicy policy) {
            this.policy = Objects.requireNonNull(policy, "policy cannot be null");
            return this;
        }

        /**
         * Builds a new context from a defensive copy of current builder state.
         */
        public RenderContext build() {
            return new RenderContext(new HashMap<>(map), policy);
        }
    }
}
