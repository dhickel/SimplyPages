package io.mindspice.demo.pages;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.layout.Page;

/**
 * Page for HTML produced by the documentation service's safe Markdown renderer.
 */
public class DocsPage implements DemoPage {

    private final String htmlContent;
    private final Component sidebar;

    public DocsPage(String htmlContent, Component sidebar) {
        this.htmlContent = htmlContent;
        this.sidebar = sidebar;
    }

    @Override
    public String render() {
        return Page.builder()
                .withStickySidebar(sidebar, 9, 3)
                .addComponents(
                    new io.mindspice.simplypages.components.Div()
                        .withAttribute("id", "docs-content").withClass("module-content")
                        .withChild(new io.mindspice.simplypages.components.RawHtml(htmlContent))
                )
                .build()
                .render();
    }

    /**
     * Renders just the content portion (for HTMX updates).
     * This is used when the sidebar navigation updates the content in-place.
     */
    public String renderContent() {
        return new io.mindspice.simplypages.components.Div()
            .withAttribute("id", "docs-content").withClass("module-content")
            .withChild(new io.mindspice.simplypages.components.RawHtml(htmlContent))
            .render();
    }
}
