package io.mindspice.simplypages.components.forms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TextAreaTest {

    @Test
    @DisplayName("TextArea should render name, rows, and value")
    void testTextAreaAttributes() {
        TextArea area = TextArea.create("bio")
            .withRows(5)
            .withValue("Hello");

        String html = area.render();

        assertTrue(html.contains("name=\"bio\""));
        assertTrue(html.contains("rows=\"5\""));
        assertTrue(html.contains(">Hello</textarea>"));
    }

    @Test
    @DisplayName("TextArea should apply additional attributes and styles")
    void testTextAreaAdditionalAttributes() {
        TextArea area = TextArea.create("notes")
            .withId("notes-id")
            .withPlaceholder("Enter notes")
            .withRows(4)
            .withCols(30)
            .required()
            .readonly()
            .disabled()
            .withMaxLength(200)
            .withMinLength(10)
            .withClass("extra")
            .withAutofocus()
            .withWrap("soft")
            .withWidth("300px")
            .withMaxWidth("600px")
            .withMinWidth("150px");

        String html = area.render();

        assertTrue(html.contains("id=\"notes-id\""));
        assertTrue(html.contains("placeholder=\"Enter notes\""));
        assertTrue(html.contains("rows=\"4\""));
        assertTrue(html.contains("cols=\"30\""));
        assertTrue(html.contains("required"));
        assertTrue(html.contains("readonly"));
        assertTrue(html.contains("disabled"));
        assertTrue(html.contains("maxlength=\"200\""));
        assertTrue(html.contains("minlength=\"10\""));
        assertTrue(html.contains("class=\"form-textarea extra\""));
        assertTrue(html.contains("autofocus"));
        assertTrue(html.contains("wrap=\"soft\""));
        assertTrue(html.contains("width: 300px;"));
        assertTrue(html.contains("max-width: 600px;"));
        assertTrue(html.contains("min-width: 150px;"));
    }
}
