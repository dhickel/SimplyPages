package io.mindspice.jhf.modules;

import io.mindspice.jhf.core.Module;

import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.components.Markdown;
import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Accordion module for collapsible content sections.
 *
 * <p>Perfect for FAQs, documentation, or any content that benefits from
 * progressive disclosure.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // FAQ accordion
 * AccordionModule.create()
 *     .withTitle("Frequently Asked Questions")
 *     .addItem("What is cannabis?", "Cannabis is a plant...")
 *     .addItem("How do I start?", "First, you need to...")
 *     .withFirstExpanded();
 *
 * // Documentation sections
 * AccordionModule.create()
 *     .addItem("Getting Started", customComponent)
 *     .addItem("Advanced Topics", moreContent);
 * }</pre>
 */
public class AccordionModule extends Module {

    public static class AccordionItem {
        private final String header;
        private final String content;
        private final Component customContent;
        private boolean expanded;

        public AccordionItem(String header, String content) {
            this.header = header;
            this.content = content;
            this.customContent = null;
            this.expanded = false;
        }

        public AccordionItem(String header, Component customContent) {
            this.header = header;
            this.content = null;
            this.customContent = customContent;
            this.expanded = false;
        }

        public String getHeader() { return header; }
        public String getContent() { return content; }
        public Component getCustomContent() { return customContent; }
        public boolean isExpanded() { return expanded; }
        public void setExpanded(boolean expanded) { this.expanded = expanded; }
    }

    private List<AccordionItem> items = new ArrayList<>();
    private boolean allowMultiple = false;

    public AccordionModule() {
        super("div");
        this.withClass("accordion-module");
    }

    public static AccordionModule create() {
        return new AccordionModule();
    }

    @Override
    public AccordionModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public AccordionModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    /**
     * Adds an accordion item with header and text content.
     *
     * @param header the accordion item header
     * @param content the content to display when expanded
     */
    public AccordionModule addItem(String header, String content) {
        this.items.add(new AccordionItem(header, content));
        return this;
    }

    /**
     * Adds an accordion item with header and custom component content.
     *
     * @param header the accordion item header
     * @param content custom component to display when expanded
     */
    public AccordionModule addItem(String header, Component content) {
        this.items.add(new AccordionItem(header, content));
        return this;
    }

    /**
     * Expands the first accordion item by default.
     */
    public AccordionModule withFirstExpanded() {
        if (!items.isEmpty()) {
            items.get(0).setExpanded(true);
        }
        return this;
    }

    /**
     * Allows multiple accordion items to be open simultaneously.
     * By default, only one item can be open at a time.
     */
    public AccordionModule allowMultiple() {
        this.allowMultiple = true;
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        Div accordion = new Div().withClass("accordion");
        if (!allowMultiple) {
            accordion.withAttribute("data-single-expand", "true");
        }

        for (int i = 0; i < items.size(); i++) {
            AccordionItem item = items.get(i);
            String itemId = (moduleId != null ? moduleId : "accordion") + "-item-" + i;

            Div accordionItem = new Div().withClass("accordion-item");

            // Header button
            HtmlTag headerBtn = new HtmlTag("button")
                .withAttribute("class", "accordion-header" + (item.isExpanded() ? " active" : ""))
                .withAttribute("type", "button")
                .withAttribute("aria-expanded", String.valueOf(item.isExpanded()))
                .withAttribute("aria-controls", itemId + "-content")
                .withInnerText(item.getHeader());

            accordionItem.withChild(headerBtn);

            // Content panel
            Div contentPanel = new Div()
                .withClass("accordion-content" + (item.isExpanded() ? " expanded" : ""))
                .withAttribute("id", itemId + "-content")
                .withAttribute("role", "region");

            if (item.getCustomContent() != null) {
                contentPanel.withChild(item.getCustomContent());
            } else if (item.getContent() != null && !item.getContent().isEmpty()) {
                HtmlTag contentText = new HtmlTag("div")
                    .withAttribute("class", "accordion-text")
                    .withInnerText(item.getContent());
                contentPanel.withChild(contentText);
            }

            accordionItem.withChild(contentPanel);
            accordion.withChild(accordionItem);
        }

        super.withChild(accordion);
    }
}
