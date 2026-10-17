package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

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

    @Test
    void defaultRoleSupportsStandaloneApplicationNamingWithoutNamingOptions() {
        RadioGroup group = RadioGroup.create("machine-field-name").addOption("a", "Alpha");
        group.withAttribute("aria-label", "Application choice");
        var template = group.compile();
        String html = group.render();
        HtmlAssert.assertThat(html)
            .attributeEquals(".form-radio-group", "role", "radiogroup")
            .attributeEquals(".form-radio-group", "aria-label", "Application choice")
            .doesNotHaveElement("input[aria-label], input[aria-labelledby]")
            .elementTextEquals("label.radio-label", "Alpha");
        assertEquals(html, template.render(RenderContext.empty()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"default", "explicit", "raw"})
    void formCaptionNamesTheGroupAndPreservesIndividualOptionLabels(String idMode) {
        RadioGroup group = RadioGroup.create("choice")
            .addOption("a", "Alpha & first")
            .addOption("b", "<Beta>")
            .withSelectedValue("b")
            .required();
        if (idMode.equals("explicit")) {
            group.withId("explicit-group");
        } else if (idMode.equals("raw")) {
            group.withAttribute("ID", "raw-group");
        }
        String previousId = group.getId();
        Form form = Form.create().addField("Choose a plan", group);
        assertEquals(previousId, group.getId());
        var template = form.compile(); // Include generated option children without a warm-up render.
        String html = form.render();
        var doc = Jsoup.parseBodyFragment(html);
        var caption = doc.selectFirst(".form-field > .form-label");
        var radios = doc.selectFirst(".form-radio-group");
        assertNotNull(caption);
        assertEquals("span", caption.tagName());
        assertFalse(caption.id().isEmpty());
        assertFalse(caption.hasAttr("for"));
        assertEquals("Choose a plan", caption.text());
        assertEquals("radiogroup", radios.attr("role"));
        assertEquals(caption.id(), radios.attr("aria-labelledby"));
        assertEquals(previousId == null ? "" : previousId, radios.id());
        assertEquals(2, radios.select("input[required]").size());
        assertEquals("b", radios.selectFirst("input[checked]").attr("value"));
        assertTrue(radios.select("input[aria-label], input[aria-labelledby]").isEmpty());
        assertEquals("Alpha & first", radios.select("label.radio-label").get(0).text());
        assertEquals("<Beta>", radios.select("label.radio-label").get(1).text());
        for (var option : radios.select(".radio-option")) {
            assertEquals(option.selectFirst("input").id(), option.selectFirst("label").attr("for"));
            assertNotEquals(caption.id(), option.selectFirst("label").attr("for"));
        }
        assertEquals(html, template.render(RenderContext.empty()));
        assertEquals(html, form.render());
        assertEquals(html, template.render(RenderContext.empty()));
    }

    @Test
    void groupCaptionAndOptionIdsRemainUniqueAndStableAcrossRepeatedNames() {
        Form first = Form.create().addField("First choice", RadioGroup.create("same").addOption("a", "A"));
        Form second = Form.create().addField("Second choice", RadioGroup.create("same").addOption("a", "A"));
        var firstTemplate = first.compile();
        var secondTemplate = second.compile();
        String html = first.render() + second.render();
        var doc = Jsoup.parseBodyFragment(html);
        var ids = doc.select("[id]").eachAttr("id");
        assertEquals(4, ids.size());
        assertEquals(ids.size(), new HashSet<>(ids).size());
        for (var field : doc.select(".form-field")) {
            var group = field.selectFirst(".form-radio-group");
            assertEquals(field.selectFirst(".form-label").id(), group.attr("aria-labelledby"));
        }
        assertEquals(html, first.render() + second.render());
        assertEquals(html, firstTemplate.render(RenderContext.empty()) + secondTemplate.render(RenderContext.empty()));
    }

    @Test
    void namedRadioFieldSnapshot() {
        RadioGroup group = RadioGroup.create("choice")
            .addOption("a", "Alpha")
            .addOption("b", "Beta")
            .withSelectedValue("b")
            .required();
        group.withId("plan-choice");
        Form form = Form.create().addField("Choose a plan", group);
        String html = form.render();
        assertEquals(html, form.compile().render(RenderContext.empty()));
        // Only random prefixes are normalized; semantic tests above verify real ID relationships.
        String snapshotHtml = html
            .replaceAll("form-label-[0-9a-f-]{36}", "form-label-generated")
            .replaceAll("radio-[0-9a-f-]{36}", "radio-generated");
        SnapshotAssert.assertMatches("forms/radio-group/named-field", snapshotHtml);
    }
}
