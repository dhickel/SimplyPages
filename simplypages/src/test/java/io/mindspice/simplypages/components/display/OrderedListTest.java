package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderedListTest {

    @Test
    @DisplayName("OrderedList should render list items")
    void testOrderedListRendering() {
        OrderedList list = OrderedList.create()
            .addItem("First")
            .addItem("Second");

        String html = list.render();

        assertTrue(html.contains("<ol"));
        assertTrue(html.contains("<li>First</li>"));
        assertTrue(html.contains("<li>Second</li>"));
    }

    @Test
    @DisplayName("OrderedList should escape text items")
    void testOrderedListEscaping() {
        OrderedList list = OrderedList.create()
            .addItem("<script>alert(1)</script>");

        String html = list.render();

        assertTrue(html.contains("&lt;script&gt;alert(1)&lt;/script&gt;"));
        assertFalse(html.contains("<script>"));
    }

    @Test
    @DisplayName("OrderedList should support component items and attributes")
    void testOrderedListComponentItem() {
        OrderedList list = OrderedList.create()
            .withStart(3)
            .reversed()
            .addItem(new Paragraph("Nested"));

        String html = list.render();

        assertTrue(html.contains("start=\"3\""));
        assertTrue(html.contains("reversed"));
        assertTrue(html.contains("Nested"));
    }
}
