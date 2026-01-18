package io.mindspice.demo.pages;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.layout.Page;
import io.mindspice.simplypages.modules.ContentModule;

/**
 * Page for rendering documentation from markdown files.
 */
public class DocsPage implements DemoPage {

    private final String markdownContent;
    private final String title;
    private final Component sidebar;

    public DocsPage(String title, String markdownContent, Component sidebar) {
        this.title = title;
        this.markdownContent = markdownContent;
        this.sidebar = sidebar;
    }

    @Override
    public String render() {
        return Page.builder()
                .withStickySidebar(sidebar, 9, 3)
                .addComponents(
                    new io.mindspice.simplypages.components.Div()
                        .withAttribute("id", "docs-content")
                        .withChild(Header.H1(title))
                        .withChild(ContentModule.create()
                            .withContent(markdownContent))
                )
                .build()
                .render();
    }

    /**
     * Renders just the content portion (for HTMX updates).
     * This is used when the sidebar navigation updates the content in-place.
     */
    public String renderContent() {
        return Header.H1(title).render()
               + ContentModule.create().withContent(markdownContent).render();
    }
}
