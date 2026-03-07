package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.AccountWidget;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a two-sided account/navigation strip component.
 *
 * <p>Contract: left and right items are emitted in insertion order under
 * {@code .account-bar-left} and {@code .account-bar-right}. If either side is empty, that
 * section is omitted.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Use one builder per composition
 * flow and avoid concurrent mutation.</p>
 */
public class AccountBarBuilder {

    private final List<Component> leftItems = new ArrayList<>();
    private final List<Component> rightItems = new ArrayList<>();
    private String customClass;
    private String backgroundColor;

    private AccountBarBuilder() {}

    /**
     * Creates a new builder.
     */
    public static AccountBarBuilder create() {
        return new AccountBarBuilder();
    }

    /**
     * Appends a component to the left section.
     */
    public AccountBarBuilder addLeftItem(Component item) {
        leftItems.add(item);
        return this;
    }

    /**
     * Appends a component to the right section.
     */
    public AccountBarBuilder addRightItem(Component item) {
        rightItems.add(item);
        return this;
    }

    /**
     * Appends a standard link to the left section.
     */
    public AccountBarBuilder addLeftLink(String text, String href) {
        leftItems.add(Link.create(href, text));
        return this;
    }

    /**
     * Appends a standard link to the right section.
     */
    public AccountBarBuilder addRightLink(String text, String href) {
        rightItems.add(Link.create(href, text));
        return this;
    }

    /**
     * Adds an extra class name to the root account bar element.
     */
    public AccountBarBuilder withClass(String className) {
        this.customClass = className;
        return this;
    }

    /**
     * Sets inline background color on the root account bar element.
     */
    public AccountBarBuilder withBackgroundColor(String color) {
        this.backgroundColor = color;
        return this;
    }

    /**
     * Appends a dynamic account widget (HTMX-backed) to the right section.
     */
    public AccountBarBuilder addRightAccountWidget(String endpoint) {
        rightItems.add(AccountWidget.createDynamic(endpoint));
        return this;
    }

    /**
     * Appends a guest account widget to the right section.
     */
    public AccountBarBuilder addRightGuestWidget() {
        rightItems.add(AccountWidget.createGuest());
        return this;
    }

    /**
     * Appends an authenticated account widget to the right section.
     */
    public AccountBarBuilder addRightAuthenticatedWidget(String username) {
        rightItems.add(AccountWidget.createAuthenticated(username));
        return this;
    }

    /**
     * Builds a new account bar component snapshot from current builder state.
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
