package io.mindspice.jhf.builders;

import io.mindspice.jhf.components.navigation.NavBar;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating top navigation bars.
 * Top nav typically contains portals/main sections.
 */
public class TopNavBuilder {

    private String brand;
    private final List<NavLink> links = new ArrayList<>();
    private String contentTarget = "#content-area";

    private TopNavBuilder() {}

    public static TopNavBuilder create() {
        return new TopNavBuilder();
    }

    public TopNavBuilder withBrand(String brand) {
        this.brand = brand;
        return this;
    }

    public TopNavBuilder addPortal(String name, String path) {
        links.add(new NavLink(name, path, false));
        return this;
    }

    public TopNavBuilder addPortal(String name, String path, boolean active) {
        links.add(new NavLink(name, path, active));
        return this;
    }

    public TopNavBuilder withContentTarget(String target) {
        this.contentTarget = target;
        return this;
    }

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
            navbar.addItem(link.name, link.path, link.active);
        }

        return navbar;
    }

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
