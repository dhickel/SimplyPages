package io.mindspice.simplypages.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A container for per-request values used to fill {@link SlotKey}s during rendering.
 * This class is mutable and intended to be request-scoped.
 */
public class RenderContext {
    public enum RenderPolicy {
        NEVER_COMPILE,
        COMPILE_ON_FIRST_HIT
    }

    private final Map<SlotKey<?>, SlotEntry> values;
    private RenderPolicy policy;

    private RenderContext(Map<SlotKey<?>, SlotEntry> values, RenderPolicy policy) {
        this.values = values;
        this.policy = policy;
    }

    /**
     * Returns an empty context.
     */
    public static RenderContext empty() {
        return new RenderContext(new HashMap<>(), RenderPolicy.NEVER_COMPILE);
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
        RenderContext context = empty();
        values.forEach((key, value) -> context.putUnchecked(key, value));
        return context;
    }

    /**
     * Retrieves the value associated with the given key.
     * If the key is not present, returns the key's default value (if any).
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

    public <T> RenderContext put(SlotKey<T> key, T value) {
        putUnchecked(key, value);
        return this;
    }

    public RenderContext putCompiled(SlotKey<?> key, String html) {
        if (html == null) {
            values.remove(key);
            return this;
        }
        values.put(key, new SlotEntry.CompiledEntry(html));
        return this;
    }

    public RenderContext remove(SlotKey<?> key) {
        values.remove(key);
        return this;
    }

    public RenderContext clear() {
        values.clear();
        return this;
    }

    public RenderContext withPolicy(RenderPolicy policy) {
        this.policy = Objects.requireNonNull(policy, "policy cannot be null");
        return this;
    }

    public RenderPolicy getPolicy() {
        return policy;
    }

    public boolean isCompiled(SlotKey<?> key) {
        return values.get(key) instanceof SlotEntry.CompiledEntry;
    }

    public Optional<String> getCompiled(SlotKey<?> key) {
        SlotEntry entry = values.get(key);
        return switch (entry) {
            case SlotEntry.CompiledEntry compiled -> Optional.ofNullable(compiled.html());
            case SlotEntry.LiveEntry ignored -> Optional.empty();
            case null -> Optional.empty();
        };
    }

    public Map<SlotKey<?>, SlotEntry> getEntries() {
        return Map.copyOf(values);
    }

    Optional<SlotEntry> getEntry(SlotKey<?> key) {
        return Optional.ofNullable(values.get(key));
    }

    private <T> void putUnchecked(SlotKey<T> key, Object value) {
        if (value == null) {
            values.remove(key);
            return;
        }
        values.put(key, new SlotEntry.LiveEntry(value.getClass(), value));
    }

    public static class RenderContextBuilder {
        private final Map<SlotKey<?>, SlotEntry> map = new HashMap<>();
        private RenderPolicy policy = RenderPolicy.NEVER_COMPILE;

        public <T> RenderContextBuilder with(SlotKey<T> key, T value) {
            if (value == null) {
                map.remove(key);
            } else {
                map.put(key, new SlotEntry.LiveEntry(value.getClass(), value));
            }
            return this;
        }

        public RenderContextBuilder withPolicy(RenderPolicy policy) {
            this.policy = Objects.requireNonNull(policy, "policy cannot be null");
            return this;
        }

        public RenderContext build() {
            return new RenderContext(new HashMap<>(map), policy);
        }
    }
}
