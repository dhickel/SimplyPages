package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.AccountWidget;
import io.mindspice.simplypages.components.Dropdown;
import io.mindspice.simplypages.components.navigation.NavBar;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Builds a top-level navigation component with primary and utility regions.
 *
 * <p>Contract: generated link entries include HTMX navigation attributes
 * (`hx-get`, `hx-target`, `hx-push-url`) targeting {@code #content-area} by default.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Intended for single request or
 * setup flow use; do not share a builder instance across threads.</p>
 */
public class TopNavBuilder {

    private String brand;
    private final List<PrimaryEntry> primaryEntries = new ArrayList<>();
    private final List<Component> utilityEntries = new ArrayList<>();
    private String contentTarget = "#content-area";
    private String customClass;
    private boolean htmxNavigationEnabled = true;

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
     * Adds a custom class to the rendered top-nav container.
     */
    public TopNavBuilder withClass(String className) {
        this.customClass = className;
        return this;
    }

    /**
     * Enables or disables HTMX navigation attributes for generated links.
     *
     * <p>When disabled, generated links are standard anchors and trigger full-page navigation.</p>
     */
    public TopNavBuilder withHtmxNavigation(boolean enabled) {
        this.htmxNavigationEnabled = enabled;
        return this;
    }

    /**
     * Adds a non-active primary link.
     */
    public TopNavBuilder addPrimaryLink(String text, String href) {
        return addPrimaryLink(text, href, false);
    }

    /**
     * Adds a primary link with explicit active state.
     */
    public TopNavBuilder addPrimaryLink(String text, String href, boolean active) {
        primaryEntries.add(new PrimaryLink(text, href, active));
        return this;
    }

    /**
     * Adds a custom primary component.
     */
    public TopNavBuilder addPrimaryComponent(Component component) {
        if (component == null) {
            throw new IllegalArgumentException("component cannot be null");
        }
        primaryEntries.add(new PrimaryComponent(component));
        return this;
    }

    /**
     * Adds a primary dropdown (left-aligned menu).
     */
    public TopNavBuilder addPrimaryDropdown(String triggerText, Consumer<Dropdown> dropdownConfig) {
        primaryEntries.add(new PrimaryComponent(buildDropdown(
            triggerText,
            "left",
            "navbar-dropdown navbar-dropdown-primary",
            dropdownConfig
        )));
        return this;
    }

    /**
     * Adds a utility link aligned to the utility region.
     */
    public TopNavBuilder addUtilityLink(String text, String href) {
        return addUtilityLink(text, href, false);
    }

    /**
     * Adds a utility link with explicit active state.
     */
    public TopNavBuilder addUtilityLink(String text, String href, boolean active) {
        HtmlTag link = buildHtmxLink(text, href, active, "navbar-item navbar-utility-item");
        utilityEntries.add(link);
        return this;
    }

    /**
     * Adds a utility dropdown (right-aligned menu).
     */
    public TopNavBuilder addUtilityDropdown(String triggerText, Consumer<Dropdown> dropdownConfig) {
        utilityEntries.add(buildDropdown(
            triggerText,
            "right",
            "navbar-dropdown navbar-dropdown-utility",
            dropdownConfig
        ));
        return this;
    }

    /**
     * Adds a custom utility component.
     */
    public TopNavBuilder addUtilityComponent(Component component) {
        if (component == null) {
            throw new IllegalArgumentException("component cannot be null");
        }
        utilityEntries.add(component);
        return this;
    }

    /**
     * Adds a guest account widget to the utility region.
     */
    public TopNavBuilder withGuestAccountWidget() {
        utilityEntries.add(AccountWidget.createGuest());
        return this;
    }

    /**
     * Adds an authenticated account widget to the utility region.
     */
    public TopNavBuilder withAuthenticatedAccountWidget(String username) {
        utilityEntries.add(AccountWidget.createAuthenticated(username));
        return this;
    }

    /**
     * Adds an HTMX-loaded account widget to the utility region.
     */
    public TopNavBuilder withDynamicAccountWidget(String endpoint) {
        utilityEntries.add(AccountWidget.createDynamic(endpoint));
        return this;
    }

    /**
     * Adds a custom account widget component to the utility region.
     */
    public TopNavBuilder withAccountWidget(Component widget) {
        if (widget == null) {
            throw new IllegalArgumentException("widget cannot be null");
        }
        utilityEntries.add(widget);
        return this;
    }

    /**
     * Legacy alias for a non-active primary link.
     */
    @Deprecated
    public TopNavBuilder addPortal(String name, String path) {
        return addPrimaryLink(name, path, false);
    }

    /**
     * Legacy alias for a primary link with active state.
     */
    @Deprecated
    public TopNavBuilder addPortal(String name, String path, boolean active) {
        return addPrimaryLink(name, path, active);
    }

    /**
     * Sets the HTMX target used for generated links.
     */
    public TopNavBuilder withContentTarget(String target) {
        this.contentTarget = target;
        return this;
    }

    /**
     * Builds a new {@link NavBar} snapshot from current builder state.
     */
    public NavBar build() {
        NavBar navbar = NavBar.create()
            .horizontal()
            .withClass("top-nav");

        if (customClass != null && !customClass.isBlank()) {
            navbar.withClass(customClass);
        }
        if (brand != null) {
            navbar.withBrand(brand);
        }

        for (PrimaryEntry entry : primaryEntries) {
            if (entry instanceof PrimaryLink link) {
                NavBar.NavItem item = new NavBar.NavItem(link.text, link.href, link.active);
                if (htmxNavigationEnabled) {
                    item.withHxGet(link.href)
                        .withHxTarget(contentTarget)
                        .withHxPushUrl();
                }
                navbar.addItem(item);
            } else if (entry instanceof PrimaryComponent component) {
                navbar.addItem(component.component);
            }
        }

        for (Component utilityEntry : utilityEntries) {
            navbar.addUtilityItem(utilityEntry);
        }

        return navbar;
    }

    private HtmlTag buildHtmxLink(String text, String href, boolean active, String className) {
        String classes = active ? className + " active" : className;
        HtmlTag link = new HtmlTag("a")
            .withAttribute("href", href == null ? "" : href)
            .withAttribute("class", classes)
            .withInnerText(text == null ? "" : text);
        if (htmxNavigationEnabled) {
            link.withAttribute("hx-get", href == null ? "" : href)
                .withAttribute("hx-target", contentTarget)
                .withAttribute("hx-push-url", "true");
        }
        return link;
    }

    private Component buildDropdown(
        String triggerText,
        String alignment,
        String className,
        Consumer<Dropdown> dropdownConfig
    ) {
        Dropdown dropdown = Dropdown.create(triggerText).withAlignment(alignment).withClass(className);
        if (dropdownConfig != null) {
            dropdownConfig.accept(dropdown);
        }
        return dropdown.build();
    }

    private sealed interface PrimaryEntry permits PrimaryLink, PrimaryComponent {}

    private record PrimaryLink(String text, String href, boolean active) implements PrimaryEntry {}

    private record PrimaryComponent(Component component) implements PrimaryEntry {}
}
