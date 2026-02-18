package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RadioGroupTest {

    @Test
    @DisplayName("RadioGroup should render options and selection")
    void testRadioGroupRendering() {
        RadioGroup group = RadioGroup.create("choice")
            .addOption("a", "Alpha")
            .addOption("b", "Beta")
            .withSelectedValue("b")
            .required()
            .inline();

        String html = group.render();

        HtmlAssert.assertThat(html)
            .hasElement("div.radio-inline")
            .hasElementCount("div.radio-inline > div.radio-option", 2)
            .attributeEquals("div.radio-option:nth-child(1) > input.radio-input", "name", "choice")
            .attributeEquals("div.radio-option:nth-child(1) > input.radio-input", "value", "a")
            .attributeEquals("div.radio-option:nth-child(2) > input.radio-input", "value", "b")
            .attributeEquals("div.radio-option:nth-child(2) > input.radio-input", "checked", "")
            .attributeEquals("div.radio-option:nth-child(1) > input.radio-input", "required", "")
            .attributeEquals("div.radio-option:nth-child(2) > input.radio-input", "required", "")
            .elementTextEquals("div.radio-option:nth-child(1) > label.radio-label", "Alpha")
            .elementTextEquals("div.radio-option:nth-child(2) > label.radio-label", "Beta");
    }

    @Test
    @DisplayName("RadioGroup should sanitize option ids and support no selection")
    void testRadioGroupIdSanitization() {
        RadioGroup group = RadioGroup.create("choice")
            .addOption("A B/C", "Alpha")
            .addOption("X", "Xray");

        String html = group.render();

        HtmlAssert.assertThat(html)
            .attributeEquals("div.radio-option:nth-child(1) > input.radio-input", "id", "choice-A-B-C")
            .attributeEquals("div.radio-option:nth-child(1) > label.radio-label", "for", "choice-A-B-C")
            .attributeEquals("div.radio-option:nth-child(2) > input.radio-input", "id", "choice-X")
            .attributeEquals("div.radio-option:nth-child(2) > label.radio-label", "for", "choice-X")
            .doesNotHaveElement("input.radio-input[checked]");
    }
}
