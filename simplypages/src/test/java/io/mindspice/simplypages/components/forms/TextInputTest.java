package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.components.forms.TextInput.InputType;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextInputTest {

    @Test
    @DisplayName("TextInput should render default type and name")
    void testDefaultTypeAndName() {
        TextInput input = TextInput.create("username");
        String html = input.render();

        HtmlAssert.assertThat(html)
            .hasElement("input.form-input")
            .attributeEquals("input.form-input", "type", "text")
            .attributeEquals("input.form-input", "name", "username");
    }

    @Test
    @DisplayName("TextInput should render custom type and value")
    void testCustomTypeAndValue() {
        TextInput input = TextInput.create("email")
            .withType(InputType.EMAIL)
            .withValue("user@example.com");

        String html = input.render();

        HtmlAssert.assertThat(html)
            .hasElement("input.form-input")
            .attributeEquals("input.form-input", "type", "email")
            .attributeEquals("input.form-input", "value", "user@example.com");
    }

    @Test
    @DisplayName("TextInput convenience constructors should set types")
    void testConvenienceConstructors() {
        HtmlAssert.assertThat(TextInput.email("email").render())
            .attributeEquals("input", "type", "email");
        HtmlAssert.assertThat(TextInput.password("password").render())
            .attributeEquals("input", "type", "password");
        HtmlAssert.assertThat(TextInput.number("age").render())
            .attributeEquals("input", "type", "number");
        HtmlAssert.assertThat(TextInput.date("birthday").render())
            .attributeEquals("input", "type", "date");
        HtmlAssert.assertThat(TextInput.search("query").render())
            .attributeEquals("input", "type", "search");
    }

    @Test
    @DisplayName("TextInput should apply additional attributes and styles on input element")
    void testAdditionalAttributes() {
        TextInput input = TextInput.create("schedule")
            .withType(InputType.DATETIME_LOCAL)
            .withId("schedule-id")
            .withValue("2024-01-01T10:00")
            .withPlaceholder("Select time")
            .required()
            .readonly()
            .disabled()
            .withPattern("\\d+")
            .withMinLength(1)
            .withMaxLength(10)
            .withMin("1")
            .withMax("10")
            .withStep("2")
            .withClass("extra")
            .withAutofocus()
            .withAutocomplete("off")
            .withWidth("120px")
            .withMaxWidth("240px")
            .withMinWidth("80px");

        String html = input.render();

        HtmlAssert.assertThat(html)
            .hasElement("input#schedule-id.form-input.extra")
            .attributeEquals("input#schedule-id", "name", "schedule")
            .attributeEquals("input#schedule-id", "type", "datetime-local")
            .attributeEquals("input#schedule-id", "value", "2024-01-01T10:00")
            .attributeEquals("input#schedule-id", "placeholder", "Select time")
            .attributeEquals("input#schedule-id", "pattern", "\\d+")
            .attributeEquals("input#schedule-id", "minlength", "1")
            .attributeEquals("input#schedule-id", "maxlength", "10")
            .attributeEquals("input#schedule-id", "min", "1")
            .attributeEquals("input#schedule-id", "max", "10")
            .attributeEquals("input#schedule-id", "step", "2")
            .attributeEquals("input#schedule-id", "autocomplete", "off")
            .attributeEquals("input#schedule-id", "required", "")
            .attributeEquals("input#schedule-id", "readonly", "")
            .attributeEquals("input#schedule-id", "disabled", "")
            .attributeEquals("input#schedule-id", "autofocus", "")
            .hasElement("input#schedule-id[style*=width]")
            .hasElement("input#schedule-id[style*=max-width]")
            .hasElement("input#schedule-id[style*=min-width]");
    }
}
