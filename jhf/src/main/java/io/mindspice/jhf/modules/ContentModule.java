package io.mindspice.jhf.modules;

import io.mindspice.jhf.core.Module;

import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.Markdown;
import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

/**
 * Module for displaying content (text, markdown, etc.)
 */
public class ContentModule extends Module {

    private String content;
    private boolean useMarkdown = true;
    private Component customContent;

    public ContentModule() {
        super("div");
        this.withClass("content-module");
    }

    public static ContentModule create() {
        return new ContentModule();
    }

    public ContentModule withContent(String content) {
        this.content = content;
        return this;
    }

    public ContentModule withCustomContent(Component content) {
        this.customContent = content;
        return this;
    }

    public ContentModule disableMarkdown() {
        this.useMarkdown = false;
        return this;
    }

    @Override
    public ContentModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public ContentModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        HtmlTag contentWrapper = new HtmlTag("div").withAttribute("class", "module-content");

        if (customContent != null) {
            contentWrapper.withChild(customContent);
        } else if (content != null) {
            if (useMarkdown) {
                contentWrapper.withChild(new Markdown(content));
            } else {
                contentWrapper.withInnerText(content);
            }
        }

        super.withChild(contentWrapper);
    }
}
