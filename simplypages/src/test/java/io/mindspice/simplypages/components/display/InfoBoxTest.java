package io.mindspice.simplypages.components.display;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InfoBoxTest {

    @Test
    @DisplayName("InfoBox should render title, value, and icon")
    void testInfoBoxRendering() {
        InfoBox box = InfoBox.create()
            .withTitle("Users")
            .withValue("42")
            .withIcon("👤");

        String html = box.render();

        assertTrue(html.contains("info-box"));
        assertTrue(html.contains("Users"));
        assertTrue(html.contains("42"));
        assertTrue(html.contains("info-box-icon"));
    }
}
