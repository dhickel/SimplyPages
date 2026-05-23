package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.Module;

import java.util.ArrayList;
import java.util.List;

/**
 * Reusable master-detail browser shell for HTMX-loaded detail fragments.
 */
public class MasterDetailBrowserModule extends Module {

    private final List<Item> items = new ArrayList<>();
    private String description;
    private String targetSelector = "#master-detail";
    private String swap = "innerHTML";
    private String activeKey;
    private Component detail;

    public MasterDetailBrowserModule() {
        super("section");
        this.withClass("master-detail-browser-module");
    }

    public static MasterDetailBrowserModule create() {
        return new MasterDetailBrowserModule();
    }

    @Override
    public MasterDetailBrowserModule withTitle(String title) {
        super.withTitle(title);
        return this;
    }

    @Override
    public MasterDetailBrowserModule withModuleId(String moduleId) {
        super.withModuleId(moduleId);
        return this;
    }

    public MasterDetailBrowserModule withDescription(String description) {
        this.description = description;
        return this;
    }

    public MasterDetailBrowserModule withTarget(String targetSelector) {
        this.targetSelector = targetSelector;
        return this;
    }

    public MasterDetailBrowserModule withSwap(String swap) {
        this.swap = swap;
        return this;
    }

    public MasterDetailBrowserModule withActiveKey(String activeKey) {
        this.activeKey = activeKey;
        return this;
    }

    public MasterDetailBrowserModule withDetail(Component detail) {
        this.detail = detail;
        return this;
    }

    public MasterDetailBrowserModule addItem(String key, String label, String endpoint) {
        return addItem(key, label, "", endpoint);
    }

    public MasterDetailBrowserModule addItem(String key, String label, String meta, String endpoint) {
        items.add(new Item(key, label, meta, endpoint));
        if (activeKey == null) {
            activeKey = key;
        }
        return this;
    }

    @Override
    protected void buildContent() {
        if (title != null && !title.isEmpty()) {
            super.withChild(Header.H2(title).withClass("module-title"));
        }
        if (description != null && !description.isEmpty()) {
            super.withChild(new HtmlTag("p")
                .withAttribute("class", "module-description")
                .withInnerText(description));
        }

        HtmlTag shell = new HtmlTag("div").withAttribute("class", "master-detail-browser-shell");
        HtmlTag list = new HtmlTag("nav")
            .withAttribute("class", "master-detail-list")
            .withAttribute("aria-label", "Items");
        for (Item item : items) {
            boolean active = item.key().equals(activeKey);
            HtmlTag button = new HtmlTag("button")
                .withAttribute("type", "button")
                .withAttribute("class", "master-detail-item" + (active ? " active" : ""))
                .withAttribute("data-item-key", item.key())
                .withAttribute("aria-current", String.valueOf(active))
                .withAttribute("hx-get", item.endpoint())
                .withAttribute("hx-target", targetSelector)
                .withAttribute("hx-swap", swap)
                .withChild(new HtmlTag("span")
                    .withAttribute("class", "master-detail-item-label")
                    .withInnerText(item.label()));
            if (!item.meta().isBlank()) {
                button.withChild(new HtmlTag("span")
                    .withAttribute("class", "master-detail-item-meta")
                    .withInnerText(item.meta()));
            }
            list.withChild(button);
        }
        shell.withChild(list);

        shell.withChild(new HtmlTag("div")
            .withAttribute("class", "master-detail-detail")
            .withAttribute("id", targetSelector.startsWith("#") ? targetSelector.substring(1) : "")
            .withChild(detail == null ? new HtmlTag("span") : detail));
        super.withChild(shell);
    }

    private record Item(String key, String label, String meta, String endpoint) {
        private Item {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key cannot be null or blank");
            }
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("label cannot be null or blank");
            }
            if (endpoint == null || endpoint.isBlank()) {
                throw new IllegalArgumentException("endpoint cannot be null or blank");
            }
            meta = meta == null ? "" : meta;
        }
    }
}
