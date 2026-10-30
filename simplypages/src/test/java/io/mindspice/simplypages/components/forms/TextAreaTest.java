package io.mindspice.simplypages.components.forms;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Slot;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.owasp.encoder.Encode;

import static org.junit.jupiter.api.Assertions.*;

class TextAreaTest {

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "plain", "\n", "\nFirst\nLine", "\n\nFirst", "First\nLine",
        "\r", "\r\nFirst", "\rFirst", "First\r\nLine\rLast", "\n\r\nFirst", "\t\nFirst",
        "\n</textarea><script>bad</script>&amp;\"'雪😀"})
    void safeValuesPreserveLeadingNewlinesInFreshStaticSnapshots(String value) {
        TextArea area = TextArea.create("text").withValue(value);
        Template template = area.compile();
        String text = value == null ? "" : value;
        String payload = (text.startsWith("\n") || text.startsWith("\r") ? "\n" : "") + Encode.forHtml(text);
        String expected = "<textarea name=\"text\" class=\"form-textarea\">" + payload + "</textarea>";
        for (int repeat = 0; repeat < 3; repeat++) {
            assertEquals(expected, template.render(RenderContext.empty()));
            assertEquals(expected, area.render());
        }
        area.withValue("changed");
        assertEquals(expected, template.render(RenderContext.empty()));
        assertTrue(area.render().endsWith(">changed</textarea>"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "plain", "\n", "\nFirst\nLine", "\n\nFirst", "First\nLine",
        "\r", "\r\nFirst", "\rFirst", "First\r\nLine\rLast", "\n\r\nFirst", "\t\nFirst",
        "\n</textarea><script>bad</script>&amp;\"'雪😀"})
    void textSlotsCompensateAtTheTagNotInTheSharedContext(String value) {
        SlotKey<String> key = SlotKey.of("text");
        HtmlTag area = TextArea.create("text").withInnerText(key);
        Template template = area.compile();
        String expected = TextArea.create("text").withValue(value).render();
        for (var policy : RenderContext.RenderPolicy.values()) {
            for (boolean compiledFirst : new boolean[] {false, true}) {
                RenderContext context = RenderContext.of(key, value).withPolicy(policy);
                for (int repeat = 0; repeat < 3; repeat++) {
                    assertEquals(expected, compiledFirst ? template.render(context) : area.render(context));
                    assertEquals(expected, compiledFirst ? area.render(context) : template.render(context));
                    assertEquals(Encode.forHtml(value), Slot.of(key).render(context));
                    HtmlTag div = new HtmlTag("div").withInnerText(key);
                    assertEquals("<div>" + Encode.forHtml(value) + "</div>", div.render(context));
                    assertEquals(div.render(context), div.compile().render(context));
                }
                if (policy == RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT) {
                    assertEquals(Encode.forHtml(value), context.getCompiled(key).orElseThrow());
                }
                context.putCompiled(key, Encode.forHtml(value));
                assertEquals(expected, area.render(context));
                assertEquals(expected, template.render(context));
                context.put(key, "replacement");
                assertTrue(template.render(context).endsWith(">replacement</textarea>"));
            }
        }
    }

    @Test
    void defaultsMissingAndSetterTransitionsKeepExistingContracts() {
        SlotKey<String> key = SlotKey.of("text", context -> "\nDefault");
        HtmlTag area = TextArea.create("text").withInnerText(key);
        Template template = area.compile();
        assertTrue(template.render(RenderContext.empty()).endsWith(">\n\nDefault</textarea>"));
        assertEquals(area.render(), template.render(RenderContext.empty()));
        area.withInnerText(SlotKey.<String>of("missing"));
        assertTrue(area.render().endsWith("></textarea>"));
        area.withUnsafeHtml("\nraw &amp;");
        assertTrue(area.render().endsWith(">\nraw &amp;</textarea>"));
        assertEquals(area.render(), area.compile().render(RenderContext.empty()));
        ((TextArea) area).withValue("\n<&");
        assertTrue(area.render().endsWith(">\n\n&lt;&amp;</textarea>"));
        assertEquals(area.render(), area.compile().render(RenderContext.empty()));
    }

    @Test
    void genericTextareaUsesHtmlSemanticsWithoutRewritingOtherTagsOrTrustedMarkup() {
        HtmlTag generic = new HtmlTag("TEXTAREA").withInnerText("\nvalue");
        assertEquals("<TEXTAREA>\n\nvalue</TEXTAREA>", generic.render());
        assertEquals(generic.render(), generic.compile().render(RenderContext.empty()));
        for (String tag : new String[] {"div", "pre", "textarea-custom"}) {
            HtmlTag other = new HtmlTag(tag).withInnerText("\nvalue");
            assertEquals("<" + tag + ">\nvalue</" + tag + ">", other.render());
            assertEquals(other.render(), other.compile().render(RenderContext.empty()));
        }
        HtmlTag raw = TextArea.create("text").withUnsafeHtml("\nvalue &amp;");
        assertTrue(raw.render().endsWith(">\nvalue &amp;</textarea>"));
        assertEquals(raw.render(), raw.compile().render(RenderContext.empty()));
        HtmlTag children = TextArea.create("text").withChild(context -> "\nraw child");
        assertTrue(children.render().endsWith(">\nraw child</textarea>"));
        assertEquals(children.render(), children.compile().render(RenderContext.empty()));
    }

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
