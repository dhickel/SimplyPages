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
 *         TopBannerBuilder.create()
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
        StringBuilder html = new StringBuilder();

        // HTML head
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"en\">\n");
        html.append("<head>\n");
        html.append("    <meta charset=\"UTF-8\">\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("    <title>").append(pageTitle).append("</title>\n");
        html.append("    <link rel=\"stylesheet\" href=\"/css/framework.css\">\n");

        if (customCss != null) {
            html.append("    <link rel=\"stylesheet\" href=\"").append(customCss).append("\">\n");
        }

        if (includeHtmx) {
            html.append("    <script src=\"/webjars/htmx.org/dist/htmx.min.js\" defer></script>\n");
        }

        html.append("    <script src=\"/js/framework.js\" defer></script>\n");

        html.append("</head>\n");
        html.append("<body>\n");

        // Top banner (header)
        if (topBanner != null) {
            HtmlTag header = new HtmlTag("header")
                .withAttribute("class", "main-header")
                .withChild(topBanner);
            html.append(header.render());
        }

        // Account bar
        if (accountBar != null) {
            html.append(accountBar.render());
        }

        // Main container
        HtmlTag mainContainer = new HtmlTag("div");
        String containerClass = "main-container";
        if (sideNav != null) {
            containerClass += " has-sidebar";
        }
        if (collapsibleSideNav) {
            containerClass += " collapsible-sidebar";
        }
        mainContainer.withAttribute("class", containerClass);

        // Side navigation
        if (sideNav != null) {
            HtmlTag aside = new HtmlTag("aside");
            String asideClass = "main-sidebar";
            if (collapsibleSideNav) {
                asideClass += " collapsible";
            }
            aside.withAttribute("class", asideClass)
                .withAttribute("id", "main-sidebar");

            // Add collapse button for collapsible sidebar
            if (collapsibleSideNav) {
                HtmlTag collapseBtn = new HtmlTag("button")
                    .withAttribute("class", "sidebar-toggle")
                    .withAttribute("onclick", "toggleSidebar()")
                    .withAttribute("aria-label", "Toggle sidebar")
                    .withInnerText("☰");
                aside.withChild(collapseBtn);
            }

            aside.withChild(sideNav);
            mainContainer.withChild(aside);
        }

        // Content wrapper
        HtmlTag contentWrapper = new HtmlTag("main")
            .withAttribute("class", "content-wrapper");

        HtmlTag contentArea = new HtmlTag("div")
            .withAttribute("id", contentTarget);

        if (includeHtmx) {
            contentArea.withAttribute("hx-get", "/home")
                .withAttribute("hx-trigger", "load");
        }

        contentWrapper.withChild(contentArea);
        mainContainer.withChild(contentWrapper);

        html.append(mainContainer.render());

        // Add collapse script if needed
        if (collapsibleSideNav) {
            html.append("""
                <script>
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
                </script>
                """);
        }

        // Add HTMX event handlers for navigation state management
        if (includeHtmx) {
            html.append("""
                <script>
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
                </script>
                """);
        }

        html.append("</body>\n");
        html.append("</html>");

        return html.toString();
    }

    /**
     * Build just the body content (for use in existing HTML templates).
     */
    public Component buildBody() {
        HtmlTag body = new HtmlTag("div")
            .withAttribute("class", "shell-body");

        if (topBanner != null) {
            HtmlTag header = new HtmlTag("header")
                .withAttribute("class", "main-header")
                .withChild(topBanner);
            body.withChild(header);
        }

        if (accountBar != null) {
            body.withChild(accountBar);
        }

        HtmlTag mainContainer = new HtmlTag("div");
        String containerClass = "main-container";
        if (sideNav != null) {
            containerClass += " has-sidebar";
        }
        if (collapsibleSideNav) {
            containerClass += " collapsible-sidebar";
        }
        mainContainer.withAttribute("class", containerClass);

        if (sideNav != null) {
            HtmlTag aside = new HtmlTag("aside");
            String asideClass = "main-sidebar";
            if (collapsibleSideNav) {
                asideClass += " collapsible";
            }
            aside.withAttribute("class", asideClass)
                .withAttribute("id", "main-sidebar");

            if (collapsibleSideNav) {
                HtmlTag collapseBtn = new HtmlTag("button")
                    .withAttribute("class", "sidebar-toggle")
                    .withAttribute("onclick", "toggleSidebar()")
                    .withInnerText("☰");
                aside.withChild(collapseBtn);
            }

            aside.withChild(sideNav);
            mainContainer.withChild(aside);
        }

        HtmlTag contentWrapper = new HtmlTag("main")
            .withAttribute("class", "content-wrapper");

        HtmlTag contentArea = new HtmlTag("div")
            .withAttribute("id", contentTarget);

        if (includeHtmx) {
            contentArea.withAttribute("hx-get", "/home")
                .withAttribute("hx-trigger", "load");
        }

        contentWrapper.withChild(contentArea);
        mainContainer.withChild(contentWrapper);

        body.withChild(mainContainer);

        return body;
    }
}
