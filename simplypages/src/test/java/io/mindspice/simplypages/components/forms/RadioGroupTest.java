package io.mindspice.simplypages.components.forms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadioGroupTest {

    @Test
    @DisplayName("RadioGroup should render options and selection")
    void testRadioGroupRendering() {
        RadioGroup group = RadioGroup.create("choice")
            .addOption("a", "Alpha")
            .addOption("b", "Beta")
            .withSelectedValue("b")
            .required()
            .inline();

        String html = group.render();

        assertTrue(html.contains("radio-inline"));
        assertTrue(html.contains("name=\"choice\""));
        assertTrue(html.contains("value=\"a\""));
        assertTrue(html.contains("value=\"b\""));
        assertTrue(html.contains("checked"));
        assertTrue(html.contains("required"));
    }

    @Test
    @DisplayName("RadioGroup should sanitize option ids and support no selection")
    void testRadioGroupIdSanitization() {
        RadioGroup group = RadioGroup.create("choice")
            .addOption("A B/C", "Alpha")
            .addOption("X", "Xray");

        String html = group.render();

        assertTrue(html.contains("id=\"choice-A-B-C\""));
        assertFalse(html.contains("checked"));
    }
}
