package io.mindspice.simplypages.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class IconTest {

    @Test
    @DisplayName("Icon should render Font Awesome classes")
    void testFontAwesomeIcon() {
        Icon icon = Icon.fontAwesome("user").large();
        String html = icon.render();

        assertTrue(html.contains("fa-user"));
        assertTrue(html.contains("icon-lg"));
    }

    @Test
    @DisplayName("Icon should render Material icon text")
    void testMaterialIcon() {
        Icon icon = Icon.material("settings");
        String html = icon.render();

        assertTrue(html.contains("material-icons"));
        assertTrue(html.contains(">settings<"));
    }

    @Test
    @DisplayName("Icon should set aria label for accessibility")
    void testIconAriaLabel() {
        Icon icon = Icon.fontAwesome("info-circle").withAriaLabel("Info");
        String html = icon.render();

        assertTrue(html.contains("aria-label=\"Info\""));
        assertTrue(html.contains("aria-hidden=\"false\""));
        assertTrue(html.contains("role=\"img\""));
    }
}
