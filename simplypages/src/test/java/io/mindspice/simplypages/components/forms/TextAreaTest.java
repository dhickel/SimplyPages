package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextAreaTest {

    @Test
    @DisplayName("TextArea should render name, rows, and value")
    void testTextAreaAttributes() {
        TextArea area = TextArea.create("bio")
            .withRows(5)
            .withValue("Hello");

        String html = area.render();

        HtmlAssert.assertThat(html)
            .hasElement("textarea.form-textarea")
            .attributeEquals("textarea.form-textarea", "name", "bio")
            .attributeEquals("textarea.form-textarea", "rows", "5")
            .elementTextEquals("textarea.form-textarea", "Hello");
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

        HtmlAssert.assertThat(html)
            .hasElement("textarea#notes-id.form-textarea.extra")
            .attributeEquals("textarea#notes-id", "name", "notes")
            .attributeEquals("textarea#notes-id", "placeholder", "Enter notes")
            .attributeEquals("textarea#notes-id", "rows", "4")
            .attributeEquals("textarea#notes-id", "cols", "30")
            .attributeEquals("textarea#notes-id", "maxlength", "200")
            .attributeEquals("textarea#notes-id", "minlength", "10")
            .attributeEquals("textarea#notes-id", "wrap", "soft")
            .attributeEquals("textarea#notes-id", "required", "")
            .attributeEquals("textarea#notes-id", "readonly", "")
            .attributeEquals("textarea#notes-id", "disabled", "")
            .attributeEquals("textarea#notes-id", "autofocus", "")
            .hasElement("textarea#notes-id[style*=width]")
            .hasElement("textarea#notes-id[style*=max-width]")
            .hasElement("textarea#notes-id[style*=min-width]");
    }
}
