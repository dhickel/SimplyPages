package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.SideNav;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a {@link SideNav} with ordered sections and links.
 *
 * <p>Contract: generated links include HTMX navigation attributes and target
 * {@code #content-area} unless overridden with {@link #withContentTarget(String)}.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Use per request/setup flow and
 * do not mutate concurrently.</p>
 */
public class SideNavBuilder {

    private final List<NavItemEntry> items = new ArrayList<>();
    private String contentTarget = "#content-area";

    private SideNavBuilder() {}

    /**
     * Creates a new builder.
     */
    public static SideNavBuilder create() {
        return new SideNavBuilder();
    }

    /**
     * Appends a section header entry.
     */
    public SideNavBuilder addSection(String title) {
        items.add(new Section(title));
        return this;
    }

    /**
     * Appends a non-active link without an icon.
     */
    public SideNavBuilder addLink(String name, String path) {
        items.add(new NavLink(name, path, false, null));
        return this;
    }

    /**
     * Appends a link with explicit active state and no icon.
     */
    public SideNavBuilder addLink(String name, String path, boolean active) {
        items.add(new NavLink(name, path, active, null));
        return this;
    }

    /**
     * Appends a non-active link with an icon.
     */
    public SideNavBuilder addLink(String name, String path, String icon) {
        items.add(new NavLink(name, path, false, icon));
        return this;
    }

    /**
     * Appends a link with explicit active state and icon.
     */
    public SideNavBuilder addLink(String name, String path, boolean active, String icon) {
        items.add(new NavLink(name, path, active, icon));
        return this;
    }

    /**
     * Sets the HTMX target used for generated links.
     */
    public SideNavBuilder withContentTarget(String target) {
        this.contentTarget = target;
        return this;
    }

    /**
     * Builds a new {@link SideNav} snapshot from current entries.
     */
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

    /** Marker type for ordered side-nav entries. */
    private sealed interface NavItemEntry permits Section, NavLink { }
    private record Section(String title) implements NavItemEntry { }
    private record NavLink(String name, String path, boolean active, String icon) implements NavItemEntry { }
}
