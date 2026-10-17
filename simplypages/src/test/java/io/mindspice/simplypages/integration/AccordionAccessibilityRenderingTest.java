package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Dropdown;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.modules.AccordionModule;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.jsoup.Jsoup;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** Generated accordion fixture consumed by the real-asset browser regressions. */
class AccordionAccessibilityRenderingTest {
    @Test
    void generatedAccordionContentAndInteractionStates() {
        var longBody = new Div()
            .withChild(new HtmlTag("p").withInnerText("Long content ".repeat(4000)))
            .withChild(Dropdown.create("Accordion actions")
                .withAlignment("left")
                .addLink("Accordion action", "#accordion-action")
                .addItem(new HtmlTag("button").withAttribute("type", "button")
                    .withInnerText("Accordion custom action"))
                .build())
            .withChild(new HtmlTag("button").withAttribute("type", "button")
                .withInnerText("Final accordion control"));

        var nested = AccordionModule.create()
            .withModuleId("nested-accordion")
            .allowMultiple()
            .addItem("Nested first", "Nested first body")
            .addItem("Nested second", "Nested second body")
            .withFirstExpanded();

        var fixture = new Div()
            .withChild(AccordionModule.create()
                .withModuleId("accordion-single")
                .addItem("Long accordion section", longBody)
                .addItem("Closed accordion section", new HtmlTag("button")
                    .withAttribute("type", "button")
                    .withInnerText("Closed section control"))
                .withFirstExpanded())
            .withChild(AccordionModule.create()
                .withModuleId("accordion-multiple")
                .allowMultiple()
                .addItem("Multiple first", "Multiple first body")
                .addItem("Multiple second", "Multiple second body"))
            .withChild(AccordionModule.create()
                .withModuleId("accordion-nested")
                .addItem("Nested outer", nested)
                .addItem("Nested sibling", "Nested sibling body")
                .withFirstExpanded());

        var identities = new LinkedHashMap<String, String>();
        var document = Jsoup.parse(fixture.render());
        document.outputSettings().prettyPrint(false);
        String html = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            .matcher(document.body().html()).replaceAll(match -> identities.computeIfAbsent(match.group(),
                ignored -> "instance-" + (identities.size() + 1)));
        SnapshotAssert.assertMatches("integration/accessibility/accordion-components", html);
    }
}
