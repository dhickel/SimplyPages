package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.modules.ContentModule;
import org.springframework.stereotype.Component;

@Component
public class HomePage implements DemoPage {
    public static final String EXAMPLE = "ContentModule.create()\n    .withTitle(\"A quieter frontend\")\n    .withContent(\"Compose **Java components**. Render HTML.\");";
    @Override public String render() {
        Div root = new Div().withClass("sp-site-home");
        root.withChild(new Div().withClass("sp-site-hero")
            .withChild(new Div().withClass("sp-site-hero-copy")
                .withChild(new Paragraph("JAVA-FIRST / SERVER-RENDERED").withClass("sp-site-eyebrow"))
                .withChild(Header.H1("Your application is Java.\nYour interface can be, too."))
                .withChild(new Paragraph("Compose a useful interface. Keep the moving parts on the server. Build with Java components, typed request data and just enough HTMX.").withClass("sp-site-lede"))
                .withChild(new Div().withClass("sp-site-actions")
                    .withChild(Link.create("/demos", "Explore working demos →").withClass("sp-site-primary"))
                    .withChild(Link.create("/docs/tutorials/first-page.md", "Build your first page"))))
            .withChild(new HtmlTag("section").withClass("sp-site-hero-note")
                .withChild(new Paragraph("THE SHORT VERSION").withClass("sp-site-eyebrow"))
                .withChild(Header.H2("Less frontend machinery.\nMore application."))
                .withChild(new Div().withClass("sp-site-flow")
                    .withChild(new Paragraph("01").withChild(new HtmlTag("strong").withInnerText("Compose in Java")))
                    .withChild(new Paragraph("02").withChild(new HtmlTag("strong").withInnerText("Render real HTML")))
                    .withChild(new Paragraph("03").withChild(new HtmlTag("strong").withInnerText("Update a fragment"))))
                .withChild(new Paragraph("No frontend framework required. No external assets on this site."))));
        root.withChild(new HtmlTag("section").withClass("sp-site-home-example")
            .withChild(new Paragraph("FROM COMPOSITION TO OUTPUT").withClass("sp-site-eyebrow"))
            .withChild(Header.H2("Java in. A real interface out."))
            .withChild(new Div().withClass("sp-site-example-grid")
                .withChild(Code.block(EXAMPLE).withLanguage("java").withTitle("Compose.java"))
                .withChild(new Div().withClass("sp-site-preview").withChild(ContentModule.create()
                    .withTitle("A quieter frontend").withContent("Compose **Java components**. Render HTML."))))
            .withChild(Link.create("/demos/modules/reading#contentmodule", "Explore content compositions →")));
        root.withChild(new HtmlTag("section").withClass("sp-site-home-section")
            .withChild(new Paragraph("A TOOLBOX, NOT A WALL OF CLASS NAMES").withClass("sp-site-eyebrow"))
            .withChild(Header.H2("Find the pieces for your next page."))
            .withChild(new Paragraph("Browse by purpose. Each family brings working output, Java composition and exact API references together."))
            .withChild(DemoNavigation.cards("Components")));
        root.withChild(new HtmlTag("section").withClass("sp-site-home-section sp-site-workflows")
            .withChild(new Paragraph("GO BEYOND THE COMPONENT").withClass("sp-site-eyebrow"))
            .withChild(Header.H2("Real workflows. Your own sandbox."))
            .withChild(new Div().withClass("sp-site-workflow-grid")
                .withChild(workflow("01 / EDIT", "Draft. Review. Approve.", "Edit nested content, validate a draft and try an approval flow.", "editing"))
                .withChild(workflow("02 / DISCUSS", "Give a conversation structure.", "Explore topics, comments, categories and tagged content.", "forum"))
                .withChild(workflow("03 / CONNECT", "Keep the conversation moving.", "Try live chat with a finite stream and explicit recovery.", "chat")))
            .withChild(new Paragraph("Temporary browser-session examples, not published collaboration. Simulated roles are not authentication. Same-origin and CSRF checks protect this site; applications own their identity, authorization, storage and resource policies.").withClass("sp-site-workflow-note")));
        root.withChild(new HtmlTag("section").withClass("sp-site-learning")
            .withChild(new Div().withChild(new Paragraph("BUILD WITH UNDERSTANDING").withClass("sp-site-eyebrow"))
                .withChild(Header.H2("From your first page\nto the finer details.")))
            .withChild(new Div().withClass("sp-site-learning-links")
                .withChild(Link.create("/docs/tutorials/README.md", "01  Learn by building →"))
                .withChild(Link.create("/docs/how-to/README.md", "02  Solve a specific task →"))
                .withChild(Link.create("/docs/explanation/README.md", "03  Understand the design →"))
                .withChild(Link.create("/api", "04  Go straight to the Java API →"))));
        return root.render();
    }
    private HtmlTag workflow(String label, String title, String body, String slug) {
        return Link.create("/demos/features/" + slug, "").withClass("sp-site-workflow-card")
            .withChild(new Paragraph(label).withClass("sp-site-eyebrow"))
            .withChild(Header.H3(title)).withChild(new Paragraph(body))
            .withChild(new HtmlTag("span").withClass("sp-site-card-action").withInnerText("Try the workflow →"));
    }
}
