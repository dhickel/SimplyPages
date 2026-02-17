package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.SideNav;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating side navigation bars.
 * Side nav typically contains sections and links within a portal.
 */
public class SideNavBuilder {

    private final List<NavItemEntry> items = new ArrayList<>();
    private String contentTarget = "#content-area";

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

    public SideNav build() {
        SideNav sideNav = SideNav.create();

        for (NavItemEntry item : items) {
            switch (item) {
                case Section section -> sideNav.addSection(section.title());
                case NavLink link -> {
                    SideNav.NavItem navItem = new SideNav.NavItem(link.name(), link.path(), link.active())
                    .withHxGet(link.path())
                    .withHxTarget(contentTarget)
                    .withHxPushUrl();

                    if (link.icon() != null) {
                        navItem.withIcon(link.icon());
                    }

                    sideNav.addItem(navItem);
                }
            }
        }

        return sideNav;
    }

    private sealed interface NavItemEntry permits Section, NavLink { }
    private record Section(String title) implements NavItemEntry { }
    private record NavLink(String name, String path, boolean active, String icon) implements NavItemEntry { }
}
