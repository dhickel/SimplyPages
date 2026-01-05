package io.mindspice.simplypages.core;

import io.mindspice.simplypages.components.Div;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotTest {

    @Test
    @DisplayName("Slot should render escaped text values")
    void testSlotEscapesText() {
        SlotKey<String> key = SlotKey.of("text");
        RenderContext context = RenderContext.builder()
            .with(key, "<b>unsafe</b>")
            .build();

        Slot<String> slot = Slot.of(key);
        String html = slot.render(context);

        assertTrue(html.contains("&lt;b&gt;unsafe&lt;/b&gt;"));
    }

    @Test
    @DisplayName("Slot should render component values")
    void testSlotRendersComponents() {
        SlotKey<Component> key = SlotKey.of("component");
        RenderContext context = RenderContext.builder()
            .with(key, new Div().withInnerText("Child"))
            .build();

        Slot<Component> slot = Slot.of(key);
        String html = slot.render(context);

        assertTrue(html.contains("Child"));
        assertTrue(html.contains("<div"));
    }

    @Test
    @DisplayName("Slot should render empty string when missing")
    void testSlotMissingValue() {
        SlotKey<String> key = SlotKey.of("missing");
        Slot<String> slot = Slot.of(key);

        assertSame(key, slot.getKey());
        assertEquals("", slot.render(RenderContext.empty()));
    }
}
