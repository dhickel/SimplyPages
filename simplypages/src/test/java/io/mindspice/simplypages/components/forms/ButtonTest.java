package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.components.forms.Button.ButtonStyle;
import io.mindspice.simplypages.components.forms.Button.ButtonType;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ButtonTest {

    @Test
    @DisplayName("Button should render default type and class")
    void testDefaultButton() {
        Button button = Button.create("Click");
        String html = button.render();

        HtmlAssert.assertThat(html)
            .hasElement("button.btn.btn-primary")
            .attributeEquals("button.btn.btn-primary", "type", "button")
            .elementTextEquals("button.btn.btn-primary", "Click");
    }

    @Test
    @DisplayName("Button should render custom type and style")
    void testCustomButton() {
        Button button = Button.create("Save")
            .withType(ButtonType.SUBMIT)
            .withStyle(ButtonStyle.DANGER);

        String html = button.render();

        HtmlAssert.assertThat(html)
            .hasElement("button.btn.btn-danger")
            .attributeEquals("button.btn.btn-danger", "type", "submit");
    }

    @Test
    @DisplayName("Button convenience methods should set types")
    void testButtonConvenienceMethods() {
        HtmlAssert.assertThat(Button.submit("Send").render())
            .attributeEquals("button", "type", "submit");
        HtmlAssert.assertThat(Button.reset("Reset").render())
            .attributeEquals("button", "type", "reset");
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

        HtmlAssert.assertThat(html)
            .hasElement("button#save-btn.btn.btn-success")
            .attributeEquals("button#save-btn", "disabled", "")
            .attributeEquals("button#save-btn", "onclick", "doSave()")
            .hasElement("button#save-btn[style*=width]")
            .hasElement("button#save-btn[style*=max-width]")
            .hasElement("button#save-btn[style*=min-width]")
            .attributeEquals("button#save-btn", "class", "btn btn-success extra btn-full-width btn-lg btn-sm");
    }
}
