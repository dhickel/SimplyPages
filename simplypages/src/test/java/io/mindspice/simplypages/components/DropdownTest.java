package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DropdownTest {

    @Test
    @DisplayName("Dropdown should render trigger and items")
    void testDropdownRendering() {
        String html = Dropdown.create("Menu")
            .addLink("Home", "/")
            .addDivider()
            .addLink("About", "/about")
            .withAlignment("left")
            .build()
            .render();

        assertTrue(html.contains("dropdown-trigger"));
        assertTrue(html.contains(">Menu</"));
        assertTrue(html.contains("dropdown-menu dropdown-align-left"));
        assertTrue(html.contains("dropdown-link"));
        assertTrue(html.contains("dropdown-divider"));
    }
}
