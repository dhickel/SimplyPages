package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.NavBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link NavBar} configured for top-level portal navigation.
 *
 * <p>Contract: each link added through this builder is emitted with HTMX navigation attributes
 * (`hx-get`, `hx-target`, `hx-push-url`) targeting {@code #content-area} by default.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Intended for single request or
 * setup flow use; do not share a builder instance across threads.</p>
 */
public class TopNavBuilder {

    private String brand;
    private final List<NavLink> links = new ArrayList<>();
    private String contentTarget = "#content-area";

    private TopNavBuilder() {}

    /**
     * Creates a new builder instance.
     */
    public static TopNavBuilder create() {
        return new TopNavBuilder();
    }

    /**
     * Sets optional navigation brand text.
     */
    public TopNavBuilder withBrand(String brand) {
        this.brand = brand;
        return this;
    }

    /**
     * Adds a non-active portal link.
     */
    public TopNavBuilder addPortal(String name, String path) {
        links.add(new NavLink(name, path, false));
        return this;
    }

    /**
     * Adds a portal link and explicit active state.
     */
    public TopNavBuilder addPortal(String name, String path, boolean active) {
        links.add(new NavLink(name, path, active));
        return this;
    }

    /**
     * Sets the HTMX target used for all generated links.
     */
    public TopNavBuilder withContentTarget(String target) {
        this.contentTarget = target;
        return this;
    }

    /**
     * Builds a new {@link NavBar} snapshot from current builder state.
     */
    public NavBar build() {
        NavBar navbar = NavBar.create().horizontal();

        if (brand != null) {
            navbar.withBrand(brand);
        }

        // Add links with HTMX integration
        for (NavLink link : links) {
            NavBar.NavItem item = new NavBar.NavItem(link.name, link.path, link.active)
                .withHxGet(link.path)
                .withHxTarget(contentTarget)
                .withHxPushUrl();
            navbar.addItem(item);
        }

        return navbar;
    }

    /**
     * Internal immutable link snapshot used when materializing the final navbar.
     */
    private static class NavLink {
        String name;
        String path;
        boolean active;

        NavLink(String name, String path, boolean active) {
            this.name = name;
            this.path = path;
            this.active = active;
        }
    }
}
