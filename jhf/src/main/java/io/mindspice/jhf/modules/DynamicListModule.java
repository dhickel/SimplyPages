package io.mindspice.jhf.modules;

import io.mindspice.jhf.core.Module;
import io.mindspice.jhf.components.Header;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic list module that can be rendered with specific items.
 * This module can be updated independently using HTMX.
 */
public class DynamicListModule extends Module {
    
    private List<String> listItems;

    public DynamicListModule() {
        super("div");
        this.withClass("list-module");
        this.listItems = new ArrayList<>();
        // Add some default items
        this.listItems.add("Item 1");
        this.listItems.add("Item 2");
        this.listItems.add("Item 3");
    }

    public static DynamicListModule create() {
        return new DynamicListModule();
    }

    public DynamicListModule withListItems(List<String> items) {
        this.listItems = items;
        return this;
    }

    @Override
    public DynamicListModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public DynamicListModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    @Override
    protected void buildContent() {
        // Add title if present
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }

        // Create list
        HtmlTag list = new HtmlTag("ul").withAttribute("class", "list-group");
        
        for (String item : listItems) {
            HtmlTag listItem = new HtmlTag("li")
                .withAttribute("class", "list-group-item")
                .withInnerText(item);
            list.withChild(listItem);
        }
        
        super.withChild(list);
    }
}