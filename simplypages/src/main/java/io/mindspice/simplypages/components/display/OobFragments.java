package io.mindspice.simplypages.components.display;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.List;

/**
 * Helpers for HTMX out-of-band fragment responses.
 */
public final class OobFragments {

    private OobFragments() {}

    public static Component swap(String id, Component content) {
        return fragment(id, "true", content);
    }

    public static Component beforeEnd(String id, Component content) {
        return fragment(id, "beforeend", content);
    }

    public static Component afterBegin(String id, Component content) {
        return fragment(id, "afterbegin", content);
    }

    public static Component delete(String id) {
        return fragment(id, "delete", new HtmlTag("span").withAttribute("hidden", "hidden"));
    }

    public static Component response(Component primary, Component... oobFragments) {
        HtmlTag root = new HtmlTag("div").withAttribute("class", "oob-fragment-response");
        if (primary != null) {
            root.withChild(primary);
        }
        if (oobFragments != null) {
            for (Component fragment : oobFragments) {
                if (fragment != null) {
                    root.withChild(fragment);
                }
            }
        }
        return root;
    }

    public static Component response(List<Component> fragments) {
        HtmlTag root = new HtmlTag("div").withAttribute("class", "oob-fragment-response");
        if (fragments != null) {
            fragments.stream()
                .filter(fragment -> fragment != null)
                .forEach(root::withChild);
        }
        return root;
    }

    private static Component fragment(String id, String swap, Component content) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id cannot be null or blank");
        }
        return new HtmlTag("div")
            .withId(id)
            .withAttribute("hx-swap-oob", swap)
            .withChild(content == null ? new HtmlTag("span") : content);
    }
}
