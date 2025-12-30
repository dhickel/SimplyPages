package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Dynamic card module that can be rendered with specific content.
 * This module can be updated independently using HTMX.
 */
public class DynamicCardModule extends Module {

    private String cardTitle;
    private String cardContent;

    public DynamicCardModule() {
        super("div");
        this.withClass("card-module");
        this.cardTitle = "Default Title";
        this.cardContent = "Default content";
    }

    public static DynamicCardModule create() {
        return new DynamicCardModule();
    }

    public DynamicCardModule withCardContent(String title, String content) {
        this.cardTitle = title;
        this.cardContent = content;
        return this;
    }

    @Override
    public DynamicCardModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public DynamicCardModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        // Add title if present
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        // Create card container
        HtmlTag card = new HtmlTag("div").withAttribute("class", "card");

        // Card header with dynamic title
        HtmlTag cardHeader = new HtmlTag("div")
            .withAttribute("class", "card-header")
            .withInnerText(cardTitle);
        card.withChild(cardHeader);

        // Card body with dynamic content
        HtmlTag cardBody = new HtmlTag("div")
            .withAttribute("class", "card-body")
            .withInnerText(cardContent);
        card.withChild(cardBody);

        super.withChild(card);
    }
}