package io.mindspice.simplypages.components.forms;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectTest {

    @Test
    @DisplayName("Select should render name and options")
    void testSelectRendersOptions() {
        Select select = Select.create("choice")
            .addOption("a", "Alpha")
            .addOption("b", "Beta", true);

        String html = select.render();

        assertTrue(html.contains("name=\"choice\""));
        assertTrue(html.contains("value=\"a\""));
        assertTrue(html.contains(">Alpha</option>"));
        assertTrue(html.contains("value=\"b\""));
        assertTrue(html.contains("selected"));
    }

    @Test
    @DisplayName("Select should render multiple and required attributes")
    void testSelectAttributes() {
        Select select = Select.create("items")
            .multiple()
            .required()
            .withSize(3);

        String html = select.render();

        assertTrue(html.contains("multiple"));
        assertTrue(html.contains("required"));
        assertTrue(html.contains("size=\"3\""));
    }

    @Test
    @DisplayName("Select should support additional attributes and list options")
    void testSelectAdditionalAttributes() {
        Select select = Select.create("items")
            .withId("items-id")
            .disabled()
            .withClass("wide")
            .addOptions(List.of("One", "Two"))
            .withWidth("300px")
            .withMaxWidth("500px")
            .withMinWidth("200px");

        String html = select.render();

        assertTrue(html.contains("id=\"items-id\""));
        assertTrue(html.contains("disabled"));
        assertTrue(html.contains("class=\"form-select wide\""));
        assertTrue(html.contains("value=\"One\""));
        assertTrue(html.contains(">One</option>"));
        assertTrue(html.contains("value=\"Two\""));
        assertTrue(html.contains(">Two</option>"));
        assertTrue(html.contains("width: 300px;"));
        assertTrue(html.contains("max-width: 500px;"));
        assertTrue(html.contains("min-width: 200px;"));
    }

    @Test
    @DisplayName("Select option should escape labels and support disabled")
    void testSelectOptionEscaping() {
        Select.Option option = new Select.Option("<script>", "<b>Label</b>", true)
            .disabled();

        String html = option.render();

        assertTrue(html.contains("selected"));
        assertTrue(html.contains("disabled"));
        assertTrue(html.contains("&lt;b&gt;Label&lt;/b&gt;"));
        assertFalse(html.contains("<script>"));
        assertFalse(html.contains("<b>"));
    }
}
