package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.SlotKeyMap;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EditFormBuilderTest {

    @Test
    @DisplayName("EditFormBuilder should create fields for common slot types")
    void testFieldCreation() {
        Map<String, SlotKey<?>> slotMapping = Map.of(
            "title", SlotKey.of("title"),
            "content", SlotKey.of("content"),
            "active", SlotKey.of("active")
        );

        Map<String, Class<?>> slotTypes = Map.of(
            "title", String.class,
            "content", String.class,
            "active", Boolean.class
        );

        SlotKeyMap values = SlotKeyMap.create()
            .putString("title", "Hello")
            .putString("content", "Details")
            .putBoolean("active", true);

        Component form = EditFormBuilder.fromSlots(slotMapping, slotTypes, values);
        String html = form.render();

        assertTrue(html.contains("name=\"title\""));
        assertTrue(html.contains("<textarea"));
        assertTrue(html.contains("name=\"content\""));
        assertTrue(html.contains("type=\"checkbox\""));
        assertTrue(html.contains("checked"));
    }
}
