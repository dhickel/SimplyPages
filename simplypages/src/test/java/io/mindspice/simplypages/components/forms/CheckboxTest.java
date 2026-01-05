package io.mindspice.simplypages.components.forms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CheckboxTest {

    @Test
    @DisplayName("Checkbox should render input, label, and checked state")
    void testCheckboxRender() {
        Checkbox checkbox = Checkbox.create("agree", "yes")
            .withLabel("Agree to terms")
            .checked();

        String html = checkbox.render();

        assertTrue(html.contains("type=\"checkbox\""));
        assertTrue(html.contains("name=\"agree\""));
        assertTrue(html.contains("value=\"yes\""));
        assertTrue(html.contains("checked"));
        assertTrue(html.contains("checkbox-label"));
        assertTrue(html.contains(">Agree to terms</label>"));
        assertTrue(html.contains("for=\"checkbox-"));
    }

    @Test
    @DisplayName("Checkbox should support required, disabled, and custom id without label")
    void testCheckboxAttributesWithoutLabel() {
        Checkbox checkbox = Checkbox.create("agree", "yes")
            .withId("agree-id")
            .required()
            .disabled();

        String html = checkbox.render();

        assertTrue(html.contains("id=\"agree-id\""));
        assertTrue(html.contains("required"));
        assertTrue(html.contains("disabled"));
        assertFalse(html.contains("checkbox-label"));
    }
}
