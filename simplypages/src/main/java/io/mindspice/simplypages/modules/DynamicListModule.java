package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.core.Module;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable list module that renders string items as a styled {@code ul}.
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. The list reference provided to
 * {@link #withListItems(List)} is used directly; mutate within a request-scoped flow. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 */
public class DynamicListModule extends Module {
    
    private List<String> listItems;

    /** Creates a module with default example items. */
    public DynamicListModule() {
        super("div");
        this.withClass("list-module");
        this.listItems = new ArrayList<>();
        // Add some default items
        this.listItems.add("Item 1");
        this.listItems.add("Item 2");
        this.listItems.add("Item 3");
    }

    /** Creates a new module instance. */
    public static DynamicListModule create() {
        return new DynamicListModule();
    }

    /** Replaces the list backing this module. */
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
