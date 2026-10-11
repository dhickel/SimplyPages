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
            .hasElement("div.form-radio-group.radio-inline")
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

        var doc = org.jsoup.Jsoup.parse(html);
        for (var option : doc.select(".radio-option")) {
            String id = option.selectFirst("input").id();
            org.junit.jupiter.api.Assertions.assertTrue(id.matches("radio-[a-zA-Z0-9-]+"));
            org.junit.jupiter.api.Assertions.assertEquals(id, option.selectFirst("label").attr("for"));
        }
        HtmlAssert.assertThat(html).doesNotHaveElement("input.radio-input[checked]");
    }

    @Test void collidingValuesAndIndependentGroupsHaveUniqueIds() {
        RadioGroup first = RadioGroup.create("choice").required().addOption("a-b", "A").addOption("a b", "B");
        RadioGroup second = RadioGroup.create("choice").addOption("a-b", "C");
        var doc = org.jsoup.Jsoup.parse(first.render() + second.render());
        var ids = doc.select("input").eachAttr("id");
        org.junit.jupiter.api.Assertions.assertEquals(3, new java.util.HashSet<>(ids).size());
        org.junit.jupiter.api.Assertions.assertEquals(2, doc.select("input[required]").size());
        org.junit.jupiter.api.Assertions.assertEquals(first.render(), first.render());
    }
}
