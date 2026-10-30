package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.layout.Page;
import org.springframework.stereotype.Component;

@Component
public class DemosOverviewPage implements DemoPage {

    @Override
    public String render() {
        Div shell = new Div().withClass("demo-overview-shell")
            .withChild(new Div().withClass("demo-overview-header-box")
                .withChild(Header.H1("SimplyPages Demo Overview").withClass("demo-overview-title"))
                .withChild(new Paragraph(""
                    + "Explore framework surfaces with practical, server-rendered examples. "
                    + "Each demo page is structured as reusable modules so you can inspect composition patterns quickly.")
                    .withClass("demo-overview-intro"))
                .withChild(tagRow("Server Rendered", "Composable Modules", "HTMX Ready")))
            .withChild(new Div().withClass("demo-overview-grid")
                .withChild(navCard("Basics & Forms", "/demos/basics-forms", "Inputs, typed controls, and end-to-end form composition.", "Components", "Forms"))
                .withChild(navCard("Display & Data", "/demos/display-data", "Status UI, data views, media, and navigation surfaces.", "Tables", "Media"))
                .withChild(navCard("Modules", "/demos/modules", "Module library examples from static blocks to dynamic render paths.", "Modules", "Patterns"))
                .withChild(navCard("HTMX & Editing", "/demos/htmx-editing", "Template + SlotKey rendering and editing integration endpoints.", "HTMX", "Editing"))
                .withChild(navCard("Chat Demo", "/chat", "Conversation timeline rendering with practical realtime update hooks.", "Chat", "Realtime"))
                .withChild(navCard("Forum Demo", "/forum", "Category, topic, and comment rendering using forum helper composition.", "Forum", "Threads")));

        return Page.builder()
            .addComponents(shell)
            .build()
            .render();
    }

    private io.mindspice.simplypages.core.Component tagRow(String... values) {
        Div row = new Div().withClass("sp-content-list-tags demo-overview-shell-tags");
        for (String value : values) {
            row.withChild(new HtmlTag("span").withClass("tag").withInnerText(value));
        }
        return row;
    }

    private io.mindspice.simplypages.core.Component navCard(String title, String href, String body, String... tags) {
        Link card = Link.create(href, "").withClass("demo-overview-card");

        Div header = new Div().withClass("demo-overview-card-header")
            .withChild(new HtmlTag("h2").withClass("demo-overview-card-title").withInnerText(title));

        Div tagRow = new Div().withClass("sp-content-list-tags demo-overview-card-tags");
        for (String tag : tags) {
            tagRow.withChild(new HtmlTag("span").withClass("tag").withInnerText(tag));
        }

        card.withChild(header)
            .withChild(tagRow)
            .withChild(new Div().withClass("demo-overview-card-main")
                .withChild(new Paragraph(body).withClass("demo-overview-card-body")))
            .withChild(new Div().withClass("demo-overview-card-footer")
                .withChild(new HtmlTag("span").withClass("demo-overview-card-cta").withInnerText("Open demo ->")));

        return card;
    }
}
