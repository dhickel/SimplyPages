package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotKeyMapTest {

    @Test
    @DisplayName("SlotKeyMap should store and retrieve typed values")
    void testPutGet() {
        SlotKeyMap map = SlotKeyMap.create()
            .putString("title", "Hello")
            .putBoolean("active", true)
            .putInt("count", 3);

        assertEquals("Hello", map.getValue("title", String.class).orElse(""));
        assertEquals(true, map.getValue("active", Boolean.class).orElse(false));
        assertEquals(3, map.getValue("count", Integer.class).orElse(0));
    }

    @Test
    @DisplayName("SlotKeyMap should merge defaults with overrides")
    void testDefaultMerging() {
        SlotKeyMap defaults = SlotKeyMap.create()
            .putString("title", "Default Title")
            .putString("content", "Default Content");

        SlotKeyMap data = SlotKeyMap.create()
            .putString("title", "Custom Title");

        SlotKeyMap merged = data.withDefaults(defaults);

        assertEquals("Custom Title", merged.getValue("title", String.class).orElse(""));
        assertEquals("Default Content", merged.getValue("content", String.class).orElse(""));
    }

    @Test
    @DisplayName("SlotKeyMap should convert to and from plain maps")
    void testConversions() {
        Map<String, Object> plain = Map.of(
            "title", "Hello",
            "count", 5
        );

        SlotKeyMap map = SlotKeyMap.fromMap(plain);
        Map<String, Object> roundTrip = map.toPlainMap();

        assertEquals("Hello", roundTrip.get("title"));
        assertEquals(5, roundTrip.get("count"));
    }

    @Test
    @DisplayName("SlotKeyMap should report empty state")
    void testEmpty() {
        SlotKeyMap map = SlotKeyMap.create();
        assertTrue(map.isEmpty());
    }

    @Test
    @DisplayName("SlotKeyMap should bridge to and from RenderContext")
    void testRenderContextBridge() {
        SlotKey<String> title = SlotKey.of("title");
        SlotKey<Integer> count = SlotKey.of("count");

        RenderContext context = RenderContext.empty()
            .put(title, "Hello")
            .put(count, 7);

        SlotKeyMap map = SlotKeyMap.fromRenderContext(context);
        assertEquals("Hello", map.getValue("title", String.class).orElse(""));
        assertEquals(7, map.getValue("count", Integer.class).orElse(0));

        RenderContext roundTrip = map.toRenderContext(Map.of(
            "title", title,
            "count", count
        ));

        assertEquals("Hello", roundTrip.get(title).orElse(""));
        assertEquals(7, roundTrip.get(count).orElse(0));
    }
}
