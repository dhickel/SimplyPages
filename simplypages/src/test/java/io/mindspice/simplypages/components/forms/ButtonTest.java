package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.components.forms.Button.ButtonStyle;
import io.mindspice.simplypages.components.forms.Button.ButtonType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ButtonTest {

    @Test
    @DisplayName("Button should render default type and class")
    void testDefaultButton() {
        Button button = Button.create("Click");
        String html = button.render();

        assertTrue(html.contains("type=\"button\""));
        assertTrue(html.contains("class=\"btn btn-primary\""));
    }

    @Test
    @DisplayName("Button should render custom type and style")
    void testCustomButton() {
        Button button = Button.create("Save")
            .withType(ButtonType.SUBMIT)
            .withStyle(ButtonStyle.DANGER);

        String html = button.render();

        assertTrue(html.contains("type=\"submit\""));
        assertTrue(html.contains("class=\"btn btn-danger\""));
    }

    @Test
    @DisplayName("Button convenience methods should set types")
    void testButtonConvenienceMethods() {
        String submitHtml = Button.submit("Send").render();
        String resetHtml = Button.reset("Reset").render();

        assertTrue(submitHtml.contains("type=\"submit\""));
        assertTrue(resetHtml.contains("type=\"reset\""));
    }

    @Test
    @DisplayName("Button should support additional attributes and sizes")
    void testButtonAdditionalAttributes() {
        Button button = Button.create("Save")
            .withStyle(ButtonStyle.SUCCESS)
            .withId("save-btn")
            .disabled()
            .withClass("extra")
            .withOnClick("doSave()")
            .fullWidth()
            .large()
            .small()
            .withWidth("120px")
            .withMaxWidth("200px")
            .withMinWidth("80px");

        String html = button.render();

        assertTrue(html.contains("class=\"btn btn-success"));
        assertTrue(html.contains("id=\"save-btn\""));
        assertTrue(html.contains("disabled"));
        assertTrue(html.contains("extra"));
        assertTrue(html.contains("onclick=\"doSave()\""));
        assertTrue(html.contains("btn-full-width"));
        assertTrue(html.contains("btn-lg"));
        assertTrue(html.contains("btn-sm"));
        assertTrue(html.contains("width: 120px;"));
        assertTrue(html.contains("max-width: 200px;"));
        assertTrue(html.contains("min-width: 80px;"));
    }
}
