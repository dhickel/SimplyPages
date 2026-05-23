package io.mindspice.simplypages.components.navigation;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

import java.util.ArrayList;
import java.util.List;

/**
 * HTMX tab navigation that swaps app-owned tab fragments into a target region.
 */
public class HtmxTabNav implements Component {

    private final List<Tab> tabs = new ArrayList<>();
    private String id;
    private String targetSelector;
    private String swap = "innerHTML";
    private boolean pushUrl;
    private String activeKey;

    public static HtmxTabNav create(String id, String targetSelector) {
        return new HtmxTabNav(id, targetSelector);
    }

    public HtmxTabNav(String id, String targetSelector) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or blank");
        }
        if (targetSelector == null || targetSelector.isBlank()) {
            throw new IllegalArgumentException("targetSelector cannot be null or blank");
        }
        this.id = id;
        this.targetSelector = targetSelector;
    }

    public HtmxTabNav addTab(String key, String label, String endpoint) {
        tabs.add(new Tab(key, label, endpoint));
        if (activeKey == null) {
            activeKey = key;
        }
        return this;
    }

    public HtmxTabNav withActiveKey(String activeKey) {
        this.activeKey = activeKey;
        return this;
    }

    public HtmxTabNav withSwap(String swap) {
        this.swap = swap;
        return this;
    }

    public HtmxTabNav withPushUrl(boolean pushUrl) {
        this.pushUrl = pushUrl;
        return this;
    }

    @Override
    public String render(RenderContext context) {
        HtmlTag nav = new HtmlTag("nav")
            .withId(id)
            .withAttribute("class", "htmx-tab-nav")
            .withAttribute("role", "tablist")
            .withAttribute("aria-label", "Tabs");

        for (Tab tab : tabs) {
            boolean active = tab.key().equals(activeKey);
            HtmlTag button = new HtmlTag("button")
                .withAttribute("type", "button")
                .withAttribute("class", "htmx-tab" + (active ? " active" : ""))
                .withAttribute("role", "tab")
                .withAttribute("aria-selected", String.valueOf(active))
                .withAttribute("data-tab-key", tab.key())
                .withAttribute("hx-get", tab.endpoint())
                .withAttribute("hx-target", targetSelector)
                .withAttribute("hx-swap", swap)
                .withAttribute("hx-push-url", String.valueOf(pushUrl))
                .withInnerText(tab.label());
            nav.withChild(button);
        }

        return nav.render(context);
    }

    private record Tab(String key, String label, String endpoint) {
        private Tab {
            if (key == null || key.isBlank()) {
                throw new IllegalArgumentException("key cannot be null or blank");
            }
            if (label == null || label.isBlank()) {
                throw new IllegalArgumentException("label cannot be null or blank");
            }
            if (endpoint == null || endpoint.isBlank()) {
                throw new IllegalArgumentException("endpoint cannot be null or blank");
            }
        }
    }
}
