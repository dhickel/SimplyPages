package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotKeyTest {

    @Test
    @DisplayName("SlotKey equality should be based on name")
    void testSlotKeyEquality() {
        SlotKey<String> keyA1 = SlotKey.of("alpha");
        SlotKey<String> keyA2 = SlotKey.of("alpha");
        SlotKey<String> keyB = SlotKey.of("beta");

        assertEquals(keyA1, keyA2);
        assertNotEquals(keyA1, keyB);
        assertEquals(keyA1.hashCode(), keyA2.hashCode());
    }

    @Test
    @DisplayName("SlotKey should expose name and defaults")
    void testSlotKeyNameAndDefaults() {
        SlotKey<String> key = SlotKey.of("title", "fallback");

        assertEquals("title", key.getName());
        assertEquals("fallback", key.getDefault(RenderContext.empty()));
        assertTrue(key.toString().contains("title"));
    }

    @Test
    @DisplayName("SlotKey should support dynamic defaults")
    void testSlotKeyDynamicDefault() {
        SlotKey<String> source = SlotKey.of("source");
        SlotKey<String> derived = SlotKey.of("derived", ctx -> ctx.get(source).orElse("none"));
        RenderContext context = RenderContext.builder().with(source, "value").build();

        assertEquals("value", derived.getDefault(context));
    }
}
