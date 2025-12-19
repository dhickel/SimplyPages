package io.mindspice.jhf.components;

import io.mindspice.jhf.core.HtmlTag;

/**
 * Account widget component for displaying user authentication status.
 * Can show login/signup links or user account information.
 * Designed to be loaded dynamically via HTMX.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Guest user (not logged in)
 * AccountWidget.createGuest()
 *     .withLoginUrl("/login")
 *     .withSignupUrl("/signup")
 *     .build();
 *
 * // Authenticated user
 * AccountWidget.createAuthenticated("john_doe")
 *     .withProfileUrl("/profile")
 *     .withLogoutUrl("/logout")
 *     .build();
 *
 * // HTMX-enabled placeholder (loads content dynamically)
 * AccountWidget.createDynamic("/api/account-status");
 * }</pre>
 */
public class AccountWidget extends HtmlTag {

    private String username;
    private String loginUrl = "/login";
    private String signupUrl = "/signup";
    private String profileUrl = "/profile";
    private String logoutUrl = "/logout";
    private boolean isAuthenticated = false;

    private AccountWidget() {
        super("div");
        this.withAttribute("class", "account-widget");
    }

    /**
     * Create a widget for guest users (not logged in).
     */
    public static AccountWidget createGuest() {
        return new AccountWidget();
    }

    /**
     * Create a widget for authenticated users.
     */
    public static AccountWidget createAuthenticated(String username) {
        AccountWidget widget = new AccountWidget();
        widget.username = username;
        widget.isAuthenticated = true;
        return widget;
    }

    /**
     * Create a dynamic widget that loads via HTMX.
     * The endpoint should return HTML for either guest or authenticated state.
     */
    public static HtmlTag createDynamic(String endpoint) {
        return new HtmlTag("div")
            .withAttribute("class", "account-widget")
            .withAttribute("hx-get", endpoint)
            .withAttribute("hx-trigger", "load")
            .withAttribute("hx-swap", "outerHTML");
    }

    /**
     * Set the login URL.
     */
    public AccountWidget withLoginUrl(String url) {
        this.loginUrl = url;
        return this;
    }

    /**
     * Set the signup URL.
     */
    public AccountWidget withSignupUrl(String url) {
        this.signupUrl = url;
        return this;
    }

    /**
     * Set the profile URL.
     */
    public AccountWidget withProfileUrl(String url) {
        this.profileUrl = url;
        return this;
    }

    /**
     * Set the logout URL.
     */
    public AccountWidget withLogoutUrl(String url) {
        this.logoutUrl = url;
        return this;
    }

    @Override
    public String render() {
        if (isAuthenticated) {
            buildAuthenticatedContent();
        } else {
            buildGuestContent();
        }
        return super.render();
    }

    /**
     * Build content for authenticated users.
     */
    private void buildAuthenticatedContent() {
        // User info container
        HtmlTag userInfo = new HtmlTag("div")
            .withAttribute("class", "account-widget-user");

        // Username/greeting
        HtmlTag greeting = new HtmlTag("span")
            .withAttribute("class", "account-widget-greeting")
            .withInnerText("Hello, " + username);
        userInfo.withChild(greeting);

        // Dropdown menu container
        HtmlTag dropdown = new HtmlTag("div")
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
        this.withChild(userInfo);
    }

    /**
     * Build content for guest users.
     */
    private void buildGuestContent() {
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

        this.withChild(guestContainer);
    }
}
