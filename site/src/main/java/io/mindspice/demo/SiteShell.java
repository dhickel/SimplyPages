package io.mindspice.demo;

import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.HtmlTag;

/** Shared application chrome; specimens retain their own framework styling. */
public final class SiteShell {
    private SiteShell() {}

    public static String render(String title, String path, String content, String... scripts) {
        HtmlTag nav = new HtmlTag("nav").withClass("sp-site-nav").withAttribute("aria-label", "Main navigation");
        String[][] links = {{"SimplyPages", "/"}, {"Demos", "/demos"}, {"Documentation", "/docs"}, {"Java API", "/api"}};
        for (String[] link : links) {
            Link item = Link.create(link[1], link[0]);
            if (path.equals(link[1]) || (!link[1].equals("/") && path.startsWith(link[1] + "/")))
                item.withAttribute("aria-current", "page");
            nav.withChild(item);
        }
        HtmlTag chrome = new HtmlTag("div").withClass("sp-site-chrome")
            .withChild(Link.create("#content-area", "Skip to content").withClass("sp-site-skip"))
            .withChild(nav);
        HtmlTag footer = new HtmlTag("footer").withClass("sp-site-footer")
            .withChild(new Paragraph("SimplyPages · Java-first server-rendered interfaces."))
            .withChild(Link.create("/docs/security/01-security-boundaries-and-safe-rendering.md", "Security boundaries"))
            .withChild(Link.create("/docs/reference/build-and-assets.md", "Build & support"));
        ShellBuilder shell = ShellBuilder.create().withPageTitle(title + " | SimplyPages")
            .withTopNav(chrome).withContentTargetClass("sp-site-main")
            .withContentWrapper(target -> ((HtmlTag) target).withAttribute("tabindex", "-1"))
            .withContent(new Div().withClass("sp-site-content").withChild(new RawHtml(content)).withChild(footer))
            .addCustomCss("/css/site.css").addCustomJs("/js/site.js")
            .addCustomJs("/js/editing-demo.js");
        for (String script : scripts) shell.addCustomJs(script);
        return shell.build();
    }

    public static String intro(String title, String description) {
        return new Div().withClass("sp-site-intro")
            .withChild(Link.create("/demos", "Demos /"))
            .withChild(Header.H1(title)).withChild(new Paragraph(description)).render();
    }

    public static String sandboxNotice() {
        return new Div().withClass("sp-site-notice")
            .withChild(new Paragraph("Your browser-session sandbox is temporary, not published, and expires after 30 idle minutes. Simulated personas and OWNER/USER modes are not authentication. Editing: 64 rows, three modules per row. Forum: 128 topics, 512 comments. At most 32 visitor sandboxes; no rate-protection or production-readiness guarantee."))
            .withChild(new HtmlTag("form").withAttribute("method", "post").withAttribute("action", "/sandbox/new")
                .withAttribute("data-sp-reset", "true")
                .withChild(new HtmlTag("button").withAttribute("type", "submit").withInnerText("Reset editing & forum sandbox")))
            .withChild(new HtmlTag("p").withAttribute("role", "status").withAttribute("data-sp-reset-status", "true"))
            .render();
    }
}
