package io.mindspice.demo.pages;

import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.layout.Page;
import io.mindspice.jhf.modules.ContentModule;

/**
 * Page for rendering documentation from markdown files.
 */
public class DocsPage implements DemoPage {

    private final String markdownContent;
    private final String title;

    public DocsPage(String title, String markdownContent) {
        this.title = title;
        this.markdownContent = markdownContent;
    }

    @Override
    public String render() {
        Page page = Page.builder()
                .addComponents(Header.H1(title))
                .addComponents(ContentModule.create()
                        .withContent(markdownContent))
                .build();

        return page.render();
    }
}
