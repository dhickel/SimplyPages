package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/** One replaceable document wrapper, with a native mobile navigation disclosure. */
public class DocsPage implements DemoPage {
    private final String html;
    private final Component navigation;
    private final String title;
    private final String path;
    public DocsPage(String html, Component navigation) { this(html, navigation, "Documentation", "README.md"); }
    public DocsPage(String html, Component navigation, String title, String path) {
        this.html = html; this.navigation = navigation; this.title = title;
        this.path = "/docs/" + (path.endsWith(".md") ? path : path + ".md");
    }
    @Override public String render() {
        return new Div().withClass("sp-site-docs")
            .withChild(new HtmlTag("aside").withClass("sp-site-docs-sidebar")
                .withChild(new HtmlTag("details").withAttribute("open", "open")
                    .withChild(new HtmlTag("summary").withInnerText("Documentation navigation"))
                    .withChild(navigation)))
            .withChild(new RawHtml(renderContent())).render();
    }
    public String renderContent() {
        return new HtmlTag("article").withId("docs-content").withClass("sp-site-docs-article")
            .withAttribute("data-doc-title", title + " | SimplyPages").withAttribute("data-doc-path", path)
            .withChild(new RawHtml(html)).render();
    }
}
