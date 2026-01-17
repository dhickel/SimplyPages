package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.core.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormFieldHelperTest {

    @Test
    @DisplayName("FormFieldHelper text field should handle null values")
    void testTextFieldNullValue() {
        Component field = FormFieldHelper.textField("Title", "title", null);

        String html = field.render();

        assertTrue(html.contains("Title:"));
        assertTrue(html.contains("name=\"title\""));
        assertTrue(html.contains(" value"));
        assertFalse(html.contains("value=\"null\""));
    }

    @Test
    @DisplayName("FormFieldHelper textarea should include rows and value")
    void testTextAreaField() {
        Component field = FormFieldHelper.textAreaField("Notes", "notes", "Value", 4);

        String html = field.render();

        assertTrue(html.contains("rows=\"4\""));
        assertTrue(html.contains(">Value<"));
    }

    @Test
    @DisplayName("FormFieldHelper textarea should handle null values")
    void testTextAreaFieldNullValue() {
        Component field = FormFieldHelper.textAreaField("Notes", "notes", null, 3);

        String html = field.render();

        assertTrue(html.contains("rows=\"3\""));
        assertFalse(html.contains("null"));
    }

    @Test
    @DisplayName("FormFieldHelper checkbox should respect checked state")
    void testCheckboxFieldChecked() {
        Component checked = FormFieldHelper.checkboxField("Enabled", "enabled", true);
        Component unchecked = FormFieldHelper.checkboxField("Enabled", "enabled", false);

        String checkedHtml = checked.render();
        String uncheckedHtml = unchecked.render();

        assertTrue(checkedHtml.contains("checked"));
        assertFalse(uncheckedHtml.contains("checked"));
    }
}
