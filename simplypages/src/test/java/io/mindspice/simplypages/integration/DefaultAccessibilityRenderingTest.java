package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.SideNavBuilder;
import io.mindspice.simplypages.components.AccountWidget;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Dropdown;
import io.mindspice.simplypages.components.display.ProgressBar;
import io.mindspice.simplypages.components.forms.*;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.AccordionModule;
import io.mindspice.simplypages.modules.TabsModule;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.Test;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

/** Reviewed server-generated fixture consumed by the real-asset browser regressions. */
class DefaultAccessibilityRenderingTest {
    @Test
    void generatedLabeledProgressStates() {
        var content = new Div();
        for (var color : ProgressBar.ColorType.values()) {
            for (String state : new String[]{"plain", "striped", "animated"}) {
                var bar = ProgressBar.create(100).withLabel(color.name() + " " + state);
                switch (color) {
                    case DEFAULT -> { }
                    case PRIMARY -> bar.primary();
                    case SUCCESS -> bar.success();
                    case WARNING -> bar.warning();
                    case ERROR -> bar.error();
                    case INFO -> bar.info();
                }
                if (state.equals("striped")) bar.striped();
                if (state.equals("animated")) bar.animated();
                content.withChild(bar);
            }
        }
        SnapshotAssert.assertMatches("integration/accessibility/labeled-progress-states", content.render());
    }

    @Test
    void generatedDefaultComponents() {
        var form = Form.create()
            .addField("Name", TextInput.create("name"))
            .addField("Notes", TextArea.create("notes"))
            .addField("Choice", Select.create("choice").addOption("one", "One"))
            .addField("Notifications", Checkbox.create("notify", "yes").withLabel("Email updates"))
            .addField("Plan", RadioGroup.create("plan").addOption("basic", "Basic").addOption("pro", "Pro"));
        var nested = TabsModule.create().withModuleId("inner-tabs")
            .addTab("Inner one", "Inner first").addTab("Inner two", "Inner second");
        var tabs = TabsModule.create().withModuleId("outer-tabs")
            .addTab("First", nested).addTab("Second", new HtmlTag("input").withAttribute("aria-label", "Second control"))
            .addTab("Third", "Third content");
        var content = new Div()
            .withChild(form)
            .withChild(ProgressBar.create(50).withLabel("Half").animated())
            .withChild(ProgressBar.create(150).withMax(200).success().striped().withLabel("Three quarters").withHeight("32px"))
            .withChild(ProgressBar.create(120).withMax(80).warning().withLabel("Complete"))
            .withChild(ProgressBar.create(25).error().withLabel("Errors"))
            .withChild(ProgressBar.create(25).info().withLabel("Information"))
            .withChild(ProgressBar.create(25).primary().withLabel("Primary"))
            .withChild(Dropdown.create("Actions").withAlignment("left").addLink("Overview", "#overview")
                .addItem(new HtmlTag("button").withAttribute("type", "button").withInnerText("Custom action")).build())
            .withChild(AccountWidget.createAuthenticated("alice"))
            .withChild(tabs)
            .withChild(AccordionModule.create().withModuleId("named-accordion")
                .addItem("Section one", "First section").addItem("Section two", "Second section").withFirstExpanded())
            .withChild(Page.builder().withStickySidebar(new HtmlTag("a").withAttribute("href", "#details")
                .withInnerText("Details link")).addComponents(new HtmlTag("p").withInnerText("Page content")).build());
        var shell = ShellBuilder.create().withHtmx(false).withContent(content)
            .withSideNav(SideNavBuilder.create().addLink("Navigation one", "#one")
                .addLink("Navigation two", "#two").build(), true);
        // Normalize UUID identity only, preserving each distinct instance and every ID relationship.
        var identities = new LinkedHashMap<String, String>();
        var document = org.jsoup.Jsoup.parse(shell.build());
        document.outputSettings().prettyPrint(false);
        String html = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
            .matcher(document.body().html()).replaceAll(match -> identities.computeIfAbsent(match.group(),
                ignored -> "instance-" + (identities.size() + 1)));
        SnapshotAssert.assertMatches("integration/accessibility/default-components", html);
    }
}
