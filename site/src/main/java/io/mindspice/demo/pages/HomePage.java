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
            .withChild(new Paragraph("JAVA-FIRST · SERVER-RENDERED · COMPOSABLE").withClass("sp-site-eyebrow"))
            .withChild(Header.H1("Your application is Java.\nYour interface can be, too."))
            .withChild(new Paragraph("Build data-heavy interfaces with Java components, typed request data, and HTML. Add HTMX where an interaction needs it, not a frontend framework around everything.").withClass("sp-site-lede"))
            .withChild(new Div().withClass("sp-site-actions")
                .withChild(Link.create("/demos", "Explore working demos →").withClass("sp-site-primary"))
                .withChild(Link.create("/docs/tutorials/first-page.md", "Build your first page"))
                .withChild(Link.create("/api", "Java API"))));
        root.withChild(new HtmlTag("section").withClass("sp-site-home-example")
            .withChild(Header.H2("Java in. A real interface out."))
            .withChild(new Div().withClass("sp-site-example-grid")
                .withChild(Code.block(EXAMPLE).withLanguage("java").withTitle("Compose.java"))
                .withChild(new Div().withClass("sp-site-preview").withChild(ContentModule.create()
                    .withTitle("A quieter frontend").withContent("Compose **Java components**. Render HTML.")))));
        root.withChild(new Div().withClass("sp-site-principles")
            .withChild(principle("01 / Compose", "Small components. Useful modules.", "Compose primitives into sections, forms, tables and complete pages. Layout stays explicit in rows and columns."))
            .withChild(principle("02 / Render", "Structure once. Values per request.", "Use SlotKey, RenderContext and compiled templates to separate reusable structure from request-time data."))
            .withChild(principle("03 / Interact", "Return HTML, not another application.", "Target a fragment with HTMX. Keep validation, permission checks and state ownership on the server.")));
        root.withChild(Header.H2("Read the code. Try the result."))
            .withChild(new Paragraph("Every catalog entry links to a live example, its Java composition, documentation and generated API."))
            .withChild(new RawHtml(SiteCatalog.directory(null)));
        root.withChild(Header.H2("Real workflows, in your own sandbox."))
            .withChild(new Div().withClass("sp-site-actions")
                .withChild(Link.create("/demos/features/editing", "Editing & proposals →"))
                .withChild(Link.create("/demos/features/forum", "Forum & tagged content →"))
                .withChild(Link.create("/demos/features/chat", "Chat & finite SSE →")))
            .withChild(new Paragraph("Temporary browser-session examples, not published collaboration. Simulated roles are not authentication. The site enforces same-origin and CSRF checks; applications still own their identity, authorization, storage and resource policies."));
        root.withChild(Header.H2("A direct path from first page to reference."))
            .withChild(new Div().withClass("sp-site-actions")
                .withChild(Link.create("/docs/tutorials/README.md", "Tutorials"))
                .withChild(Link.create("/docs/how-to/README.md", "How-to guides"))
                .withChild(Link.create("/docs/reference/README.md", "Reference"))
                .withChild(Link.create("/docs/explanation/README.md", "Explanation")));
        return root.render();
    }
    private Div principle(String label, String title, String body) {
        return new Div().withChild(new Paragraph(label).withClass("sp-site-eyebrow"))
            .withChild(Header.H2(title)).withChild(new Paragraph(body));
    }
}
