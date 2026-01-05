package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.components.forms.TextInput.InputType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TextInputTest {

    @Test
    @DisplayName("TextInput should render default type and name")
    void testDefaultTypeAndName() {
        TextInput input = TextInput.create("username");
        String html = input.render();

        assertTrue(html.contains("type=\"text\""));
        assertTrue(html.contains("name=\"username\""));
    }

    @Test
    @DisplayName("TextInput should render custom type and value")
    void testCustomTypeAndValue() {
        TextInput input = TextInput.create("email")
            .withType(InputType.EMAIL)
            .withValue("user@example.com");

        String html = input.render();

        assertTrue(html.contains("type=\"email\""));
        assertTrue(html.contains("value=\"user@example.com\""));
    }

    @Test
    @DisplayName("TextInput convenience constructors should set types")
    void testConvenienceConstructors() {
        String emailHtml = TextInput.email("email").render();
        String passwordHtml = TextInput.password("password").render();
        String numberHtml = TextInput.number("age").render();
        String dateHtml = TextInput.date("birthday").render();
        String searchHtml = TextInput.search("query").render();

        assertTrue(emailHtml.contains("type=\"email\""));
        assertTrue(passwordHtml.contains("type=\"password\""));
        assertTrue(numberHtml.contains("type=\"number\""));
        assertTrue(dateHtml.contains("type=\"date\""));
        assertTrue(searchHtml.contains("type=\"search\""));
    }

    @Test
    @DisplayName("TextInput should apply additional attributes and styles")
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

        assertTrue(html.contains("type=\"datetime-local\""));
        assertTrue(html.contains("id=\"schedule-id\""));
        assertTrue(html.contains("value=\"2024-01-01T10:00\""));
        assertTrue(html.contains("placeholder=\"Select time\""));
        assertTrue(html.contains("required"));
        assertTrue(html.contains("readonly"));
        assertTrue(html.contains("disabled"));
        assertTrue(html.contains("pattern=\"\\d+\""));
        assertTrue(html.contains("minlength=\"1\""));
        assertTrue(html.contains("maxlength=\"10\""));
        assertTrue(html.contains("min=\"1\""));
        assertTrue(html.contains("max=\"10\""));
        assertTrue(html.contains("step=\"2\""));
        assertTrue(html.contains("class=\"form-input extra\""));
        assertTrue(html.contains("autofocus"));
        assertTrue(html.contains("autocomplete=\"off\""));
        assertTrue(html.contains("width: 120px;"));
        assertTrue(html.contains("max-width: 240px;"));
        assertTrue(html.contains("min-width: 80px;"));
    }
}
