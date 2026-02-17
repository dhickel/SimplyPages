package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.SideNav;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

/**
 * Comprehensive builder for creating the entire application shell/layout.
 * Supports top banner, account bar, side navigation, and main content area.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * ShellBuilder.create()
 *     .withTopBanner(
 *         BannerBuilder.create()
 *             .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
 *             .withTitle("My App")
 *             .withImage("/logo.png", "Logo")
 *             .build()
 *     )
 *     .withAccountBar(
 *         AccountBarBuilder.create()
 *             .addLeftLink("Home", "/")
 *             .addRightLink("Login", "/login")
 *             .build()
 *     )
 *     .withSideNav(
 *         SideNavBuilder.create()
 *             .addSection("Main")
 *             .addLink("Dashboard", "/dashboard")
 *             .build(),
 *         true  // collapsible
 *     )
 *     .withContentTarget("content-area")
 *     .build();
 * }</pre>
 */
public class ShellBuilder {

    private Component topBanner;
    private Component accountBar;
    private SideNav sideNav;
    private boolean collapsibleSideNav = false;
    private String contentTarget = "content-area";
    private String pageTitle = "Application";
    private boolean includeHtmx = true;
    private String customCss;

    private ShellBuilder() {}

    public static ShellBuilder create() {
        return new ShellBuilder();
    }

    /**
     * Set the top banner component.
     */
    public ShellBuilder withTopBanner(Component topBanner) {
        this.topBanner = topBanner;
        return this;
    }

    /**
     * Set the account bar component (displayed under the top banner).
     */
    public ShellBuilder withAccountBar(Component accountBar) {
        this.accountBar = accountBar;
        return this;
    }

    /**
     * Set the side navigation component.
     */
    public ShellBuilder withSideNav(SideNav sideNav) {
        this.sideNav = sideNav;
        return this;
    }

    /**
     * Set the side navigation with collapsible option.
     */
    public ShellBuilder withSideNav(SideNav sideNav, boolean collapsible) {
        this.sideNav = sideNav;
        this.collapsibleSideNav = collapsible;
        return this;
    }

    /**
     * Enable/disable collapsible side navigation.
     */
    public ShellBuilder withCollapsibleSideNav(boolean collapsible) {
        this.collapsibleSideNav = collapsible;
        return this;
    }

    /**
     * Set the content area target ID for HTMX swaps.
     */
    public ShellBuilder withContentTarget(String targetId) {
        this.contentTarget = targetId;
        return this;
    }

    /**
     * Set the page title.
     */
    public ShellBuilder withPageTitle(String title) {
        this.pageTitle = title;
        return this;
    }

    /**
     * Enable/disable HTMX inclusion.
     */
    public ShellBuilder withHtmx(boolean include) {
        this.includeHtmx = include;
        return this;
    }

    /**
     * Add custom CSS file path.
     */
    public ShellBuilder withCustomCss(String cssPath) {
        this.customCss = cssPath;
        return this;
    }

    /**
     * Build the complete shell HTML.
     * This generates a full HTML document string.
     */
    public String build() {
        HtmlTag html = new HtmlTag("html").withAttribute("lang", "en");

        HtmlTag head = new HtmlTag("head")
            .withChild(new HtmlTag("meta", true).withAttribute("charset", "UTF-8"))
            .withChild(new HtmlTag("meta", true).withAttribute("name", "viewport")
                .withAttribute("content", "width=device-width, initial-scale=1.0"))
            .withChild(new HtmlTag("title").withInnerText(pageTitle))
            .withChild(new HtmlTag("link", true).withAttribute("rel", "stylesheet")
                .withAttribute("href", "/css/framework.css"));

        if (customCss != null) {
            head.withChild(new HtmlTag("link", true).withAttribute("rel", "stylesheet")
                .withAttribute("href", customCss));
        }
        if (includeHtmx) {
            head.withChild(new HtmlTag("script")
                .withAttribute("src", "/webjars/htmx.org/dist/htmx.min.js")
                .withAttribute("defer", ""));
        }
        head.withChild(new HtmlTag("script")
            .withAttribute("src", "/js/framework.js")
            .withAttribute("defer", ""));

        HtmlTag body = buildShellContent();
        appendInlineScripts(body);

        html.withChild(head).withChild(body);
        return "<!DOCTYPE html>\n" + html.render();
    }

    /**
     * Build just the body content (for use in existing HTML templates).
     */
    public Component buildBody() {
        return new HtmlTag("div")
            .withAttribute("class", "shell-body")
            .withChild(buildMainContainer(false));
    }

    private HtmlTag buildShellContent() {
        HtmlTag body = new HtmlTag("body");

        if (topBanner != null) {
            body.withChild(new HtmlTag("header")
                .withAttribute("class", "main-header")
                .withChild(topBanner));
        }
        if (accountBar != null) {
            body.withChild(accountBar);
        }

        body.withChild(buildMainContainer(true));
        return body;
    }

    private HtmlTag buildMainContainer(boolean includeAriaLabel) {
        HtmlTag mainContainer = new HtmlTag("div").withAttribute("class", getContainerClass());

        if (sideNav != null) {
            HtmlTag aside = new HtmlTag("aside")
                .withAttribute("class", getAsideClass())
                .withAttribute("id", "main-sidebar");

            if (collapsibleSideNav) {
                HtmlTag collapseBtn = new HtmlTag("button")
                    .withAttribute("class", "sidebar-toggle")
                    .withAttribute("onclick", "toggleSidebar()")
                    .withInnerText("☰");
                if (includeAriaLabel) {
                    collapseBtn.withAttribute("aria-label", "Toggle sidebar");
                }
                aside.withChild(collapseBtn);
            }

            aside.withChild(sideNav);
            mainContainer.withChild(aside);
        }

        HtmlTag contentArea = new HtmlTag("div").withAttribute("id", contentTarget);
        if (includeHtmx) {
            contentArea.withAttribute("hx-get", "/home")
                .withAttribute("hx-trigger", "load");
        }

        mainContainer.withChild(new HtmlTag("main")
            .withAttribute("class", "content-wrapper")
            .withChild(contentArea));
        return mainContainer;
    }

    private void appendInlineScripts(HtmlTag body) {
        if (collapsibleSideNav) {
            body.withChild(new HtmlTag("script").withUnsafeHtml("""
                function toggleSidebar() {
                    const sidebar = document.getElementById('main-sidebar');
                    const container = document.querySelector('.main-container');
                    sidebar.classList.toggle('collapsed');
                    container.classList.toggle('sidebar-collapsed');

                    // Save state to localStorage
                    const isCollapsed = sidebar.classList.contains('collapsed');
                    localStorage.setItem('sidebarCollapsed', isCollapsed);
                }

                // Restore sidebar state on page load
                document.addEventListener('DOMContentLoaded', function() {
                    const isCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';
                    if (isCollapsed) {
                        document.getElementById('main-sidebar').classList.add('collapsed');
                        document.querySelector('.main-container').classList.add('sidebar-collapsed');
                    }
                });
                """));
        }

        if (includeHtmx) {
            body.withChild(new HtmlTag("script").withUnsafeHtml("""
                // Update active navigation link when content changes
                document.body.addEventListener('htmx:afterSettle', function(event) {
                    // Remove active class from all nav items
                    document.querySelectorAll('.sidenav-item').forEach(item => {
                        item.classList.remove('active');
                    });

                    // Add active class to current page's nav item
                    const currentPath = window.location.pathname;
                    const activeLink = document.querySelector('.sidenav-item[href="' + currentPath + '"]');
                    if (activeLink) {
                        activeLink.classList.add('active');
                    }
                });
                """));
        }
    }

    private String getContainerClass() {
        String containerClass = "main-container";
        if (sideNav != null) {
            containerClass += " has-sidebar";
        }
        if (collapsibleSideNav) {
            containerClass += " collapsible-sidebar";
        }
        return containerClass;
    }

    private String getAsideClass() {
        String asideClass = "main-sidebar";
        if (collapsibleSideNav) {
            asideClass += " collapsible";
        }
        return asideClass;
    }
}
