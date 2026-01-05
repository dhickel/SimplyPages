package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributeTest {

    @Test
    @DisplayName("Attribute should render boolean attributes without values")
    void testBooleanAttribute() {
        Attribute attr = new Attribute("disabled", "");
        String html = attr.render();

        assertTrue(html.equals(" disabled") || html.contains(" disabled"));
    }

    @Test
    @DisplayName("Attribute should escape values")
    void testAttributeEscaping() {
        Attribute attr = new Attribute("data-test", "\" onload=\"alert(1)");
        String html = attr.render();

        assertTrue(html.contains("data-test=\""));
        assertTrue(html.contains("&#34;") || html.contains("&quot;"));
        assertFalse(html.contains("\" onload="));
    }
}
