package io.mindspice.simplypages.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Container for slot data with runtime type safety and default merging.
 */
public class SlotKeyMap {
    private final Map<String, TypedValue> values;

    private SlotKeyMap() {
        this.values = new HashMap<>();
    }

    public static SlotKeyMap create() {
        return new SlotKeyMap();
    }

    public SlotKeyMap putString(String slotName, String value) {
        values.put(slotName, TypedValue.string(value));
        return this;
    }

    public SlotKeyMap putBoolean(String slotName, Boolean value) {
        values.put(slotName, TypedValue.bool(value));
        return this;
    }

    public SlotKeyMap putInt(String slotName, Integer value) {
        values.put(slotName, TypedValue.integer(value));
        return this;
    }

    @SuppressWarnings("unchecked")
    public SlotKeyMap put(String slotName, Class<?> type, Object value) {
        values.put(slotName, TypedValue.of((Class<Object>) type, value));
        return this;
    }

    public Optional<TypedValue> get(String slotName) {
        return Optional.ofNullable(values.get(slotName));
    }

    public <T> Optional<T> getValue(String slotName, Class<T> expectedType) {
        return get(slotName).map(tv -> tv.getValueAs(expectedType));
    }

    @SuppressWarnings("unchecked")
    public <T> T getOrDefault(String slotName, T defaultValue) {
        return (T) get(slotName).map(TypedValue::getValue).orElse(defaultValue);
    }

    /**
     * Merge with defaults. This map's values take precedence.
     */
    public SlotKeyMap withDefaults(SlotKeyMap defaults) {
        SlotKeyMap merged = new SlotKeyMap();
        defaults.values.forEach(merged.values::put);
        this.values.forEach(merged.values::put);
        return merged;
    }

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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putContextValue(RenderContext context, SlotKey<?> key, Object value) {
        context.put((SlotKey) key, value);
    }

    public Map<String, Object> toPlainMap() {
        Map<String, Object> plain = new HashMap<>();
        values.forEach((key, tv) -> plain.put(key, tv.value()));
        return plain;
    }

    public Map<String, TypedValue> getValues() {
        return Map.copyOf(values);
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public int size() {
        return values.size();
    }
}
