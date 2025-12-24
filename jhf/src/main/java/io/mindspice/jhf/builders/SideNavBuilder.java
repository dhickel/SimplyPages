package io.mindspice.jhf.builders;

import io.mindspice.jhf.components.navigation.SideNav;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating side navigation bars.
 * Side nav typically contains sections and links within a portal.
 */
public class SideNavBuilder {

    private final List<Object> items = new ArrayList<>(); // Can be NavLink or Section
    private String contentTarget = "#content-area";
    private String hxSwap;

    private SideNavBuilder() {}

    public static SideNavBuilder create() {
        return new SideNavBuilder();
    }

    public SideNavBuilder addSection(String title) {
        items.add(new Section(title));
        return this;
    }

    public SideNavBuilder addLink(String name, String path) {
        items.add(new NavLink(name, path, false, null));
        return this;
    }

    public SideNavBuilder addLink(String name, String path, boolean active) {
        items.add(new NavLink(name, path, active, null));
        return this;
    }

    public SideNavBuilder addLink(String name, String path, String icon) {
        items.add(new NavLink(name, path, false, icon));
        return this;
    }

    public SideNavBuilder addLink(String name, String path, boolean active, String icon) {
        items.add(new NavLink(name, path, active, icon));
        return this;
    }

    public SideNavBuilder withContentTarget(String target) {
        this.contentTarget = target;
        return this;
    }

    public SideNavBuilder withHxSwap(String swap) {
        this.hxSwap = swap;
        return this;
    }

    public SideNav build() {
        SideNav sideNav = SideNav.create();

        for (Object item : items) {
            if (item instanceof Section section) {
                sideNav.addSection(section.title);
            } else if (item instanceof NavLink link) {
                SideNav.NavItem navItem = new SideNav.NavItem(link.name, link.path, link.active)
                    .withHxGet(link.path)
                    .withHxTarget(contentTarget)
                    .withHxSwap(hxSwap)
                    .withHxPushUrl();

                if (link.icon != null) {
                    navItem.withIcon(link.icon);
                }

                sideNav.addItem(navItem);
            }
        }

        return sideNav;
    }

    private static class Section {
        String title;

        Section(String title) {
            this.title = title;
        }
    }

    private static class NavLink {
        String name;
        String path;
        boolean active;
        String icon;

        NavLink(String name, String path, boolean active, String icon) {
            this.name = name;
            this.path = path;
            this.active = active;
            this.icon = icon;
        }
    }
}
