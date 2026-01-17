package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.AccountWidget;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder for creating account/user bars.
 * Typically displayed under the top banner for navigation links and user authentication.
 * Functions as a horizontal flexbox (hbox) with left and right sections.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple account bar with links
 * AccountBarBuilder.create()
 *     .addLeftLink("Home", "/")
 *     .addLeftLink("About", "/about")
 *     .addRightAccountWidget("/api/account-status")
 *     .build();
 *
 * // Account bar with custom components
 * AccountBarBuilder.create()
 *     .addLeftItem(Link.create("/", "Home"))
 *     .addRightItem(AccountWidget.createGuest())
 *     .build();
 * }</pre>
 */
public class AccountBarBuilder {

    private final List<Component> leftItems = new ArrayList<>();
    private final List<Component> rightItems = new ArrayList<>();
    private String customClass;
    private String backgroundColor;

    private AccountBarBuilder() {}

    public static AccountBarBuilder create() {
        return new AccountBarBuilder();
    }

    /**
     * Add an item to the left side of the account bar.
     */
    public AccountBarBuilder addLeftItem(Component item) {
        leftItems.add(item);
        return this;
    }

    /**
     * Add an item to the right side of the account bar.
     */
    public AccountBarBuilder addRightItem(Component item) {
        rightItems.add(item);
        return this;
    }

    /**
     * Add a link to the left side.
     */
    public AccountBarBuilder addLeftLink(String text, String href) {
        leftItems.add(Link.create(href, text));
        return this;
    }

    /**
     * Add a link to the right side.
     */
    public AccountBarBuilder addRightLink(String text, String href) {
        rightItems.add(Link.create(href, text));
        return this;
    }

    /**
     * Add custom CSS class to the account bar.
     */
    public AccountBarBuilder withClass(String className) {
        this.customClass = className;
        return this;
    }

    /**
     * Set custom background color.
     */
    public AccountBarBuilder withBackgroundColor(String color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Add an account widget to the right side that loads dynamically via HTMX.
     * Convenience method for common use case.
     */
    public AccountBarBuilder addRightAccountWidget(String endpoint) {
        rightItems.add(AccountWidget.createDynamic(endpoint));
        return this;
    }

    /**
     * Add a guest account widget to the right side.
     */
    public AccountBarBuilder addRightGuestWidget() {
        rightItems.add(AccountWidget.createGuest());
        return this;
    }

    /**
     * Add an authenticated account widget to the right side.
     */
    public AccountBarBuilder addRightAuthenticatedWidget(String username) {
        rightItems.add(AccountWidget.createAuthenticated(username));
        return this;
    }

    /**
     * Build the account bar component.
     */
    public Component build() {
        HtmlTag accountBar = new HtmlTag("div");
        accountBar.withAttribute("class", "account-bar" + (customClass != null ? " " + customClass : ""));

        if (backgroundColor != null) {
            accountBar.withAttribute("style", "background-color: " + backgroundColor);
        }

        // Left section
        if (!leftItems.isEmpty()) {
            HtmlTag leftSection = new HtmlTag("div")
                .withAttribute("class", "account-bar-left");
            leftItems.forEach(leftSection::withChild);
            accountBar.withChild(leftSection);
        }

        // Right section
        if (!rightItems.isEmpty()) {
            HtmlTag rightSection = new HtmlTag("div")
                .withAttribute("class", "account-bar-right");
            rightItems.forEach(rightSection::withChild);
            accountBar.withChild(rightSection);
        }

        return accountBar;
    }
}
