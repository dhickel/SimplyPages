package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for dropdown-style menu markup.
 *
 * <p>Mutable and not thread-safe. Items and configuration are accumulated on this builder until
 * {@link #build()} is called.</p>
 */
public class Dropdown {

    private String triggerText;
    private final List<Component> items = new ArrayList<>();
    private String customClass;
    private String alignment = "right"; // left, right, center

    /**
     * Creates a dropdown builder.
     *
     * @param triggerText visible trigger text
     */
    private Dropdown(String triggerText) {
        this.triggerText = triggerText;
    }

    /**
     * Creates a dropdown builder.
     *
     * @param triggerText visible trigger text
     * @return new dropdown builder
     */
    public static Dropdown create(String triggerText) {
        return new Dropdown(triggerText);
    }

    /**
     * Appends an anchor item.
     *
     * @param text link text
     * @param href link href
     * @return this dropdown builder
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
     * Appends a custom menu item component.
     *
     * @param component menu item component
     * @return this dropdown builder
     */
    public Dropdown addItem(Component component) {
        items.add(component);
        return this;
    }

    /**
     * Appends a divider item.
     *
     * @return this dropdown builder
     */
    public Dropdown addDivider() {
        HtmlTag divider = new HtmlTag("div")
            .withAttribute("class", "dropdown-divider");
        items.add(divider);
        return this;
    }

    /**
     * Sets additional container class token(s).
     *
     * @param className class token(s)
     * @return this dropdown builder
     */
    public Dropdown withClass(String className) {
        this.customClass = className;
        return this;
    }

    /**
     * Sets dropdown alignment token.
     *
     * @param alignment expected tokens: {@code left}, {@code right}, or {@code center}
     * @return this dropdown builder
     */
    public Dropdown withAlignment(String alignment) {
        this.alignment = alignment;
        return this;
    }

    /**
     * Builds and returns a static dropdown component tree.
     *
     * @return root dropdown component
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
