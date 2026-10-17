package io.mindspice.simplypages.core;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.components.forms.Select;
import io.mindspice.simplypages.layout.Column;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReviewRegressionTest {
    @Test void templatesPreserveGeneratedChildrenAndCustomRenderers() {
        Component[] components = {
            Select.create("choice").addOption("a", "Alpha"),
            Card.create().withHeader("Title").withBody("Body"),
            DataTable.create(String.class).addColumn("Name", value -> value).withData(java.util.List.of("Ada")),
            ProgressBar.create(40),
            io.mindspice.simplypages.components.forms.Checkbox.create("enabled", "yes").withLabel("Enabled"),
            io.mindspice.simplypages.components.forms.RadioGroup.create("choice").addOption("a", "Alpha"),
            Spinner.create().withMessage("Loading"),
            Column.create().withChild(new Paragraph("Column")),
            new Blockquote("Quote"),
            Header.H2("Heading").withBottomBar(),
            Code.block("<code>").withTitle("Example"),
            Modal.create().withModalId("dialog").withBody(new Paragraph("Body"))
        };
        for (Component component : components) {
            Template template = component.compile(); // Compile before any render side effects.
            assertEquals(component.render(), template.render(RenderContext.empty()), component.getClass().getName());
        }
    }

    @Test void nestedComponentsRetainContentAndPresentation() {
        var document = Jsoup.parse(new Div()
            .withChild(Code.block("<sample>").withTitle("<img src=x onerror=alert(1)>"))
            .withChild(new Paragraph("centered").center())
            .withChild(Spinner.create().withMessage("Waiting"))
            .withChild(Column.create().withChild(new Paragraph("column"))).render());
        assertEquals("<sample>", document.select("pre code").text());
        assertEquals("<img src=x onerror=alert(1)>", document.select(".code-title").text());
        assertTrue(document.select(".code-title img").isEmpty());
        assertEquals("centered", document.select("p.align-center").text());
        assertEquals("Waiting", document.select(".spinner-message").text());
        assertEquals("column", document.select(".col").text());
    }

    @Test void modalPassesContextToBodyAndFooter() {
        SlotKey<String> key = SlotKey.of("modal-value");
        Modal modal = Modal.create().withBody(Slot.of(key)).withFooter(Slot.of(key));
        var document = Jsoup.parse(modal.render(RenderContext.of(key, "<value>")));
        assertEquals("<value>", document.select(".modal-body").text());
        assertEquals("<value>", document.select(".modal-footer").text());
    }

    @Test void styleReplacementMatchesWholePropertyNames() {
        String style = Jsoup.parse(new HtmlTag("div").withMinWidth("10px")
            .withMaxWidth("90px").withWidth("50px").withWidth("60px").render())
            .selectFirst("div").attr("style");
        assertTrue(style.contains("min-width: 10px;"));
        assertTrue(style.contains("max-width: 90px;"));
        assertTrue(style.contains("width: 60px;"));
        assertFalse(style.contains("width: 50px;"));
    }

    @Test void replacingTextSlotWithHtmlAndClearingIdWorks() {
        assertEquals("<div></div>", new HtmlTag("div").withInnerText((String) null).render());
        assertEquals("<div></div>", new HtmlTag("div").withUnsafeHtml(null).compile().render(RenderContext.empty()));
        HtmlTag tag = new HtmlTag("div").withId("old").withId(null)
            .withInnerText(SlotKey.<String>of("old-text")).withUnsafeHtml("<b>new</b>");
        var element = Jsoup.parse(tag.render()).selectFirst("div");
        assertFalse(element.hasAttr("id"));
        assertEquals("new", element.select("b").text());
    }

    @Test void compiledEntriesRemainVisibleInDirectSlotRendering() {
        SlotKey<String> key = SlotKey.of("shared");
        RenderContext context = RenderContext.of(key, "<value>")
            .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT);
        String expected = Slot.of(key).compile().render(context);
        assertEquals(expected, Slot.of(key).render(context));
        assertEquals("<p>" + expected + "</p>", new HtmlTag("p").withInnerText(key).render(context));
    }

    @Test void invalidNamesCannotBreakOutOfMarkup() {
        for (String name : new String[] {"x onclick", "x=1", "x>"}) {
            assertThrows(IllegalArgumentException.class, () -> new Attribute(name, "value"));
            assertThrows(IllegalArgumentException.class, () -> new HtmlTag(name));
        }
        HtmlTag tag = new HtmlTag("div").withAttribute("ID", "old").withAttribute("id", "new");
        assertEquals("new", tag.getId());
        HtmlTag uppercase = new HtmlTag("div").withAttribute("ID", "upper");
        assertEquals(uppercase.render(), uppercase.compile().render(RenderContext.empty()));
        assertEquals(1, Jsoup.parse(tag.render()).selectFirst("div").attributes().size());
    }

    @Test void failedModuleBuildCanRetryWithoutDuplicateChildren() {
        Module module = new Module("section") {
            boolean first = true;
            protected void buildContent() {
                withChild(new Paragraph("one"));
                if (first) {
                    first = false;
                    withId("failed-id");
                    throw new IllegalStateException("temporary failure");
                }
            }
        };
        assertThrows(IllegalStateException.class, module::build);
        assertEquals(1, Jsoup.parse(module.render()).select("p").size());
        assertNull(module.getId());
    }

}
