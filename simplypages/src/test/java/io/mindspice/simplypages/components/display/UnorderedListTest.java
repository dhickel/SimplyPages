package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnorderedListTest {

    @Test
    @DisplayName("UnorderedList should render list items")
    void testUnorderedListRendering() {
        UnorderedList list = UnorderedList.create()
            .addItem("One")
            .addItem(new Paragraph("Two"));

        String html = list.render();

        assertTrue(html.contains("<ul"));
        assertTrue(html.contains("<li>One</li>"));
        assertTrue(html.contains("Two"));
    }

    @Test
    @DisplayName("UnorderedList should escape text items")
    void testUnorderedListEscaping() {
        UnorderedList list = UnorderedList.create()
            .addItem("<img src=x onerror=alert(1)>");

        String html = list.render();

        assertTrue(html.contains("&lt;img src=x onerror=alert(1)&gt;"));
        assertFalse(html.contains("<img"));
    }

    @Test
    @DisplayName("UnorderedList should apply unstyled and inline classes")
    void testUnorderedListStyles() {
        UnorderedList list = UnorderedList.create()
            .unstyled()
            .inline()
            .addItem("Item");

        String html = list.render();

        assertTrue(html.contains("list-unstyled"));
        assertTrue(html.contains("list-inline"));
    }
}
