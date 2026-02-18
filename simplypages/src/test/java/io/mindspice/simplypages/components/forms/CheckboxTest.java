package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckboxTest {

    @Test
    @DisplayName("Checkbox should render input, label, and checked state")
    void testCheckboxRender() {
        Checkbox checkbox = Checkbox.create("agree", "yes")
            .withLabel("Agree to terms")
            .checked();

        String html = checkbox.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.form-checkbox > input.checkbox-input")
            .hasElement("div.form-checkbox > label.checkbox-label")
            .attributeEquals("input.checkbox-input", "type", "checkbox")
            .attributeEquals("input.checkbox-input", "name", "agree")
            .attributeEquals("input.checkbox-input", "value", "yes")
            .attributeEquals("input.checkbox-input", "checked", "")
            .elementTextEquals("label.checkbox-label", "Agree to terms");

        String inputId = org.jsoup.Jsoup.parseBodyFragment(html).selectFirst("input.checkbox-input").attr("id");
        HtmlAssert.assertThat(html)
            .attributeEquals("label.checkbox-label", "for", inputId);
    }

    @Test
    @DisplayName("Checkbox should support required, disabled, and custom id without label")
    void testCheckboxAttributesWithoutLabel() {
        Checkbox checkbox = Checkbox.create("agree", "yes")
            .withId("agree-id")
            .required()
            .disabled();

        String html = checkbox.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.form-checkbox > input#agree-id.checkbox-input")
            .attributeEquals("input#agree-id", "required", "")
            .attributeEquals("input#agree-id", "disabled", "")
            .doesNotHaveElement("label.checkbox-label");
    }
}
