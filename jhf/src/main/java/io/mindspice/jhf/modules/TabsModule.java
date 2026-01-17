package io.mindspice.jhf.modules;

import io.mindspice.jhf.components.Div;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabs module for organizing content into tabbed panels.
 *
 * <p>Tabs are useful for grouping related content and allowing users to
 * switch between different views without leaving the page.</p>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Basic tabs
 * TabsModule.create()
 *     .withTitle("Research Data")
 *     .addTab("Overview", "Summary of research findings...")
 *     .addTab("Methods", "Research methodology...")
 *     .addTab("Results", customComponent);
 *
 * // Tabs with custom styling
 * TabsModule.create()
 *     .addTab("Tab 1", content1)
 *     .addTab("Tab 2", content2)
 *     .withActiveTab(1); // Make second tab active
 * }</pre>
 */
public class TabsModule extends Module {

    public static class Tab {
        private final String label;
        private final String content;
        private final Component customContent;
        private boolean active;

        public Tab(String label, String content) {
            this.label = label;
            this.content = content;
            this.customContent = null;
            this.active = false;
        }

        public Tab(String label, Component customContent) {
            this.label = label;
            this.content = null;
            this.customContent = customContent;
            this.active = false;
        }

        public String getLabel() { return label; }
        public String getContent() { return content; }
        public Component getCustomContent() { return customContent; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    private List<Tab> tabs = new ArrayList<>();

    public TabsModule() {
        super("div");
        this.withClass("tabs-module");
    }

    public static TabsModule create() {
        return new TabsModule();
    }

    @Override
    public TabsModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public TabsModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    /**
     * Adds a tab with label and text content.
     *
     * @param label the tab label
     * @param content the content to display in this tab
     */
    public TabsModule addTab(String label, String content) {
        this.tabs.add(new Tab(label, content));
        // First tab is active by default
        if (tabs.size() == 1) {
            tabs.get(0).setActive(true);
        }
        return this;
    }

    /**
     * Adds a tab with label and custom component content.
     *
     * @param label the tab label
     * @param content custom component to display in this tab
     */
    public TabsModule addTab(String label, Component content) {
        this.tabs.add(new Tab(label, content));
        // First tab is active by default
        if (tabs.size() == 1) {
            tabs.get(0).setActive(true);
        }
        return this;
    }

    /**
     * Sets which tab is active by default.
     *
     * @param index zero-based index of the tab to activate
     */
    public TabsModule withActiveTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            // Deactivate all tabs
            for (Tab tab : tabs) {
                tab.setActive(false);
            }
            // Activate the specified tab
            tabs.get(index).setActive(true);
        }
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        Div tabsContainer = new Div().withClass("tabs-container");

        // Tab navigation
        HtmlTag tabNav = new HtmlTag("ul")
            .withAttribute("class", "tab-nav")
            .withAttribute("role", "tablist");

        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            String tabId = (moduleId != null ? moduleId : "tabs") + "-tab-" + i;
            String panelId = (moduleId != null ? moduleId : "tabs") + "-panel-" + i;

            HtmlTag tabItem = new HtmlTag("li")
                .withAttribute("role", "presentation")
                .withAttribute("class", "tab-item");

            HtmlTag tabButton = new HtmlTag("button")
                .withAttribute("class", "tab-button" + (tab.isActive() ? " active" : ""))
                .withAttribute("id", tabId)
                .withAttribute("role", "tab")
                .withAttribute("aria-selected", String.valueOf(tab.isActive()))
                .withAttribute("aria-controls", panelId)
                .withInnerText(tab.getLabel());

            tabItem.withChild(tabButton);
            tabNav.withChild(tabItem);
        }

        tabsContainer.withChild(tabNav);

        // Tab panels
        Div tabPanels = new Div().withClass("tab-panels");

        for (int i = 0; i < tabs.size(); i++) {
            Tab tab = tabs.get(i);
            String tabId = (moduleId != null ? moduleId : "tabs") + "-tab-" + i;
            String panelId = (moduleId != null ? moduleId : "tabs") + "-panel-" + i;

            Div panel = new Div()
                .withClass("tab-panel" + (tab.isActive() ? " active" : ""))
                .withAttribute("id", panelId)
                .withAttribute("role", "tabpanel")
                .withAttribute("aria-labelledby", tabId);

            if (tab.getCustomContent() != null) {
                panel.withChild(tab.getCustomContent());
            } else if (tab.getContent() != null && !tab.getContent().isEmpty()) {
                HtmlTag contentText = new HtmlTag("div")
                    .withAttribute("class", "tab-content")
                    .withInnerText(tab.getContent());
                panel.withChild(contentText);
            }

            tabPanels.withChild(panel);
        }

        tabsContainer.withChild(tabPanels);
        super.withChild(tabsContainer);
    }
}
