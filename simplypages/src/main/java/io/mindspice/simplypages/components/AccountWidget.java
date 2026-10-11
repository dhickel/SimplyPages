package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;
import java.util.stream.Stream;

/**
 * Account-state widget that renders either guest links or authenticated user actions.
 *
 * <p>Configuration is mutable and not thread-safe. Rendering generates state-derived content
 * without modifying stored children, and forwards context to inherited children. Configure before
 * sharing; custom child delegates and request contexts have their own sharing requirements.</p>
 * <p>Authenticated actions use disclosure navigation, not an ARIA menu. Load framework JS/CSS
 * once in the shell for native-button activation and dismissal.</p>
 */
public class AccountWidget extends HtmlTag {

    private String username;
    private String loginUrl = "/login";
    private String signupUrl = "/signup";
    private String profileUrl = "/profile";
    private String logoutUrl = "/logout";
    private boolean isAuthenticated = false;
    private final String dropdownId = "account-actions-" + java.util.UUID.randomUUID();

    /**
     * Creates a widget root with class {@code account-widget}.
     */
    private AccountWidget() {
        super("div");
        this.withAttribute("class", "account-widget");
    }

    /**
     * Creates a guest-state widget.
     *
     * @return guest widget
     */
    public static AccountWidget createGuest() {
        return new AccountWidget();
    }

    /**
     * Creates an authenticated-state widget.
     *
     * @param username display name
     * @return authenticated widget
     */
    public static AccountWidget createAuthenticated(String username) {
        AccountWidget widget = new AccountWidget();
        widget.username = username;
        widget.isAuthenticated = true;
        return widget;
    }

    /**
     * Creates an HTMX placeholder that loads widget HTML from an endpoint.
     *
     * @param endpoint HTMX GET endpoint
     * @return placeholder div
     */
    public static HtmlTag createDynamic(String endpoint) {
        return new HtmlTag("div")
            .withAttribute("class", "account-widget")
            .withAttribute("hx-get", endpoint)
            .withAttribute("hx-trigger", "load")
            .withAttribute("hx-swap", "outerHTML");
    }

    /**
     * Sets login URL.
     *
     * @param url login URL
     * @return this widget
     */
    public AccountWidget withLoginUrl(String url) {
        this.loginUrl = url;
        return this;
    }

    /**
     * Sets signup URL.
     *
     * @param url signup URL
     * @return this widget
     */
    public AccountWidget withSignupUrl(String url) {
        this.signupUrl = url;
        return this;
    }

    /**
     * Sets profile URL.
     *
     * @param url profile URL
     * @return this widget
     */
    public AccountWidget withProfileUrl(String url) {
        this.profileUrl = url;
        return this;
    }

    /**
     * Sets logout URL.
     *
     * @param url logout URL
     * @return this widget
     */
    public AccountWidget withLogoutUrl(String url) {
        this.logoutUrl = url;
        return this;
    }

    /** Generates account content followed by inherited children without mutating either. */
    @Override
    protected Stream<Component> getChildrenStream() {
        Component account = isAuthenticated ? buildAuthenticatedContent() : buildGuestContent();
        return Stream.concat(Stream.of(account), super.getChildrenStream());
    }

    /**
     * Builds child nodes for authenticated state.
     */
    private Component buildAuthenticatedContent() {
        // User info container
        HtmlTag userInfo = new HtmlTag("div")
            .withAttribute("class", "account-widget-user")
            .withAttribute("data-sp-disclosure", "");

        // Username/greeting
        HtmlTag greeting = new HtmlTag("button")
            .withAttribute("type", "button")
            .withAttribute("class", "account-widget-greeting")
            .withAttribute("data-sp-disclosure-toggle", "")
            .withAttribute("aria-controls", dropdownId)
            .withAttribute("aria-expanded", "false")
            .withInnerText("Hello, " + username);
        userInfo.withChild(greeting);

        // Dropdown menu container
        HtmlTag dropdown = new HtmlTag("div")
            .withId(dropdownId)
            .withAttribute("hidden", "")
            .withAttribute("class", "account-widget-dropdown");

        // Profile link
        HtmlTag profileLink = new HtmlTag("a")
            .withAttribute("href", profileUrl)
            .withAttribute("class", "account-widget-link")
            .withInnerText("Profile");
        dropdown.withChild(profileLink);

        // Logout link
        HtmlTag logoutLink = new HtmlTag("a")
            .withAttribute("href", logoutUrl)
            .withAttribute("class", "account-widget-link")
            .withInnerText("Logout");
        dropdown.withChild(logoutLink);

        userInfo.withChild(dropdown);
        return userInfo;
    }

    /**
     * Builds child nodes for guest state.
     */
    private Component buildGuestContent() {
        HtmlTag guestContainer = new HtmlTag("div")
            .withAttribute("class", "account-widget-guest");

        // Login link
        HtmlTag loginLink = new HtmlTag("a")
            .withAttribute("href", loginUrl)
            .withAttribute("class", "account-widget-link account-widget-login")
            .withInnerText("Login");
        guestContainer.withChild(loginLink);

        // Signup link (optional)
        if (signupUrl != null) {
            HtmlTag signupLink = new HtmlTag("a")
                .withAttribute("href", signupUrl)
                .withAttribute("class", "account-widget-link account-widget-signup")
                .withInnerText("Sign Up");
            guestContainer.withChild(signupLink);
        }

        return guestContainer;
    }
}
