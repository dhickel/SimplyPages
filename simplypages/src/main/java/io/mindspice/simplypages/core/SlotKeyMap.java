package io.mindspice.simplypages.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Mutable string-keyed slot value map used mainly as a legacy bridge API.
 *
 * <p>This map stores values as {@link TypedValue} to preserve runtime type metadata. For modern
 * rendering, prefer {@link RenderContext} and {@link SlotKey} directly.</p>
 *
 * <p>Mutability/thread-safety: mutable and not thread-safe. Treat as request-scoped data. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 */
public class SlotKeyMap {
    /** Backing mutable map keyed by slot name. */
    private final Map<String, TypedValue> values;

    /**
     * Creates an empty map.
     */
    private SlotKeyMap() {
        this.values = new HashMap<>();
    }

    /**
     * Factory for an empty slot map.
     */
    public static SlotKeyMap create() {
        return new SlotKeyMap();
    }

    /**
     * Stores a string value for {@code slotName}.
     */
    public SlotKeyMap putString(String slotName, String value) {
        values.put(slotName, TypedValue.string(value));
        return this;
    }

    /**
     * Stores a boolean value for {@code slotName}.
     */
    public SlotKeyMap putBoolean(String slotName, Boolean value) {
        values.put(slotName, TypedValue.bool(value));
        return this;
    }

    /**
     * Stores an integer value for {@code slotName}.
     */
    public SlotKeyMap putInt(String slotName, Integer value) {
        values.put(slotName, TypedValue.integer(value));
        return this;
    }

    /**
     * Stores {@code value} with an explicit runtime type token.
     *
     * @param slotName slot name
     * @param type expected runtime type of value
     * @param value value to store; may be {@code null}
     * @return this map
     */
    @SuppressWarnings("unchecked")
    public SlotKeyMap put(String slotName, Class<?> type, Object value) {
        values.put(slotName, TypedValue.of((Class<Object>) type, value));
        return this;
    }

    /**
     * Returns the typed value wrapper for {@code slotName}, when present.
     */
    public Optional<TypedValue> get(String slotName) {
        return Optional.ofNullable(values.get(slotName));
    }

    /**
     * Returns the value for {@code slotName} cast as {@code expectedType}.
     */
    public <T> Optional<T> getValue(String slotName, Class<T> expectedType) {
        return get(slotName).map(tv -> tv.getValueAs(expectedType));
    }

    /**
     * Returns the stored value, or {@code defaultValue} when absent.
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String slotName, T defaultValue) {
        return (T) get(slotName).map(TypedValue::getValue).orElse(defaultValue);
    }

    /**
     * Returns a merged copy where this map overrides {@code defaults} for duplicate keys.
     */
    public SlotKeyMap withDefaults(SlotKeyMap defaults) {
        SlotKeyMap merged = new SlotKeyMap();
        defaults.values.forEach(merged.values::put);
        this.values.forEach(merged.values::put);
        return merged;
    }

    /**
     * Converts a plain object map into a typed slot map.
     *
     * <p>Entries with {@code null} values are skipped.</p>
     */
    public static SlotKeyMap fromMap(Map<String, ?> plainMap) {
        SlotKeyMap map = new SlotKeyMap();
        plainMap.forEach((key, value) -> {
            if (value != null) {
                map.put(key, value.getClass(), value);
            }
        });
        return map;
    }

    /**
     * Legacy bridge: convert explicit entries from a render context into a string-key map.
     * Slot names are derived from {@link SlotKey#name()}.
     */
    public static SlotKeyMap fromRenderContext(RenderContext context) {
        SlotKeyMap map = new SlotKeyMap();
        context.getEntries().forEach((slotKey, entry) -> {
            String name = slotKey.name();
            switch (entry) {
                case SlotEntry.LiveEntry live -> {
                    if (live.value() != null) {
                        map.put(name, live.type(), live.value());
                    }
                }
                case SlotEntry.CompiledEntry compiled -> map.putString(name, compiled.html());
            }
        });
        return map;
    }

    /**
     * Legacy bridge: convert this string-key map to a render context using the provided slot key mapping.
     * Entries with no matching key are ignored.
     */
    public RenderContext toRenderContext(Map<String, SlotKey<?>> slotMapping) {
        RenderContext context = RenderContext.empty();
        values.forEach((name, typedValue) -> {
            SlotKey<?> key = slotMapping.get(name);
            if (key != null) {
                putContextValue(context, key, typedValue.value());
            }
        });
        return context;
    }

    /**
     * Bridge helper to pass a value through generic {@link RenderContext#put(SlotKey, Object)}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putContextValue(RenderContext context, SlotKey<?> key, Object value) {
        context.put((SlotKey) key, value);
    }

    /**
     * Returns a mutable plain object copy of this map's values.
     */
    public Map<String, Object> toPlainMap() {
        Map<String, Object> plain = new HashMap<>();
        values.forEach((key, tv) -> plain.put(key, tv.value()));
        return plain;
    }

    /**
     * Returns an unmodifiable snapshot of internal typed values.
     */
    public Map<String, TypedValue> getValues() {
        return Map.copyOf(values);
    }

    /**
     * Returns whether the map has no entries.
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Returns the number of stored entries.
     */
    public int size() {
        return values.size();
    }
}
