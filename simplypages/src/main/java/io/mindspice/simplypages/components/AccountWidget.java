package io.mindspice.simplypages.components;

import io.mindspice.simplypages.core.HtmlTag;
import io.mindspice.simplypages.core.RenderContext;

/**
 * Account-state widget that renders either guest links or authenticated user actions.
 *
 * <p>Mutable and not thread-safe. Render methods rebuild child content on each call based on
 * current authentication state fields. For reuse, stop mutating shared instances and render stable structures with per-request context data.</p>
 */
public class AccountWidget extends HtmlTag {

    private String username;
    private String loginUrl = "/login";
    private String signupUrl = "/signup";
    private String profileUrl = "/profile";
    private String logoutUrl = "/logout";
    private boolean isAuthenticated = false;

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

    /**
     * Rebuilds children for current auth state and renders root container.
     *
     * @param context render context
     * @return widget HTML
     */
    @Override
    public String render(RenderContext context) {
        children.clear();
        if (isAuthenticated) {
            buildAuthenticatedContent();
        } else {
            buildGuestContent();
        }
        return super.render(context);
    }

    /**
     * Renders using empty context.
     *
     * @return widget HTML
     */
    @Override
    public String render() {
        return render(RenderContext.empty());
    }

    /**
     * Builds child nodes for authenticated state.
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
     * Builds child nodes for guest state.
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
