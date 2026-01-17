package io.mindspice.jhf.components;

import io.mindspice.jhf.core.Component;
import io.mindspice.jhf.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic dropdown component for creating hover-based dropdown menus.
 * Can be used for navigation dropdowns, user menus, or any other dropdown needs.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Navigation dropdown
 * Dropdown.create("Services")
 *     .addLink("Consulting", "/services/consulting")
 *     .addLink("Development", "/services/development")
 *     .addDivider()
 *     .addLink("Contact Us", "/contact")
 *     .build();
 *
 * // Custom dropdown with components
 * Dropdown dropdown = Dropdown.create("Account")
 *     .addItem(new Paragraph().withInnerText("Hello, user"))
 *     .addLink("Profile", "/profile")
 *     .addLink("Logout", "/logout")
 *     .build();
 * }</pre>
 */
public class Dropdown {

    private String triggerText;
    private final List<Component> items = new ArrayList<>();
    private String customClass;
    private String alignment = "right"; // left, right, center

    private Dropdown(String triggerText) {
        this.triggerText = triggerText;
    }

    /**
     * Create a new dropdown with the given trigger text.
     */
    public static Dropdown create(String triggerText) {
        return new Dropdown(triggerText);
    }

    /**
     * Add a link item to the dropdown.
     */
    public Dropdown addLink(String text, String href) {
        HtmlTag link = new HtmlTag("a")
            .withAttribute("href", href)
            .withAttribute("class", "dropdown-link")
            .withInnerText(text);
        items.add(link);
        return this;
    }

    /**
     * Add a custom component to the dropdown.
     */
    public Dropdown addItem(Component component) {
        items.add(component);
        return this;
    }

    /**
     * Add a divider line between dropdown items.
     */
    public Dropdown addDivider() {
        HtmlTag divider = new HtmlTag("div")
            .withAttribute("class", "dropdown-divider");
        items.add(divider);
        return this;
    }

    /**
     * Set custom CSS class for the dropdown container.
     */
    public Dropdown withClass(String className) {
        this.customClass = className;
        return this;
    }

    /**
     * Set the alignment of the dropdown menu.
     * @param alignment "left", "right", or "center" (default: "right")
     */
    public Dropdown withAlignment(String alignment) {
        this.alignment = alignment;
        return this;
    }

    /**
     * Build the dropdown component.
     */
    public Component build() {
        HtmlTag container = new HtmlTag("div");
        container.withAttribute("class", "dropdown" + (customClass != null ? " " + customClass : ""));

        // Trigger element
        HtmlTag trigger = new HtmlTag("span")
            .withAttribute("class", "dropdown-trigger")
            .withInnerText(triggerText);
        container.withChild(trigger);

        // Dropdown menu
        HtmlTag menu = new HtmlTag("div")
            .withAttribute("class", "dropdown-menu dropdown-align-" + alignment);

        // Add all items to the menu
        items.forEach(menu::withChild);

        container.withChild(menu);

        return container;
    }
}
