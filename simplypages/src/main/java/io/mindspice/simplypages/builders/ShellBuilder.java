package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.navigation.SideNav;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.HtmlTag;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Builds a full SimplyPages shell document (head + body) or body-only fragment.
 *
 * <p>Contract: output shape remains stable for integrators:
 * optional header/account regions, optional side nav, and a main content target container.</p>
 *
 * <p>Mutability and thread-safety: mutable and not thread-safe. Configure once per render flow
 * and do not share mutable instances across concurrent requests.</p>
 */
public class ShellBuilder {

    private Component topBanner;
    private Component accountBar;
    private SideNav sideNav;
    private boolean collapsibleSideNav = false;
    private String contentTarget = "content-area";
    private String pageTitle = "Application";
    private boolean includeHtmx = true;
    private boolean includeFrameworkCss = true;
    private String frameworkCssPath = "/css/framework.css";
    private final Set<String> customCssPaths = new LinkedHashSet<>();
    private final Set<String> customJsPaths = new LinkedHashSet<>();
    private String contentTargetClass;
    private Function<Component, Component> contentWrapper = Function.identity();
    private Component content;

    private ShellBuilder() {}

    /**
     * Creates a new builder.
     */
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
     * Alias for {@link #withContentTarget(String)}.
     */
    public ShellBuilder withContentTargetId(String targetId) {
        return withContentTarget(targetId);
    }

    /**
     * Set optional class on the content target div.
     */
    public ShellBuilder withContentTargetClass(String className) {
        this.contentTargetClass = requireNonBlank(className, "className");
        return this;
    }

    /**
     * Apply a wrapper around the content target component.
     */
    public ShellBuilder withContentWrapper(Function<Component, Component> wrapper) {
        if (wrapper == null) {
            throw new IllegalArgumentException("wrapper cannot be null");
        }
        this.contentWrapper = wrapper;
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
     * Sets initial shell content rendered inside the configured content target.
     *
     * <p>When initial content is provided, the content target is rendered without the
     * default HTMX auto-load attributes.</p>
     */
    public ShellBuilder withContent(Component content) {
        this.content = content;
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
        this.customCssPaths.clear();
        this.customCssPaths.add(requireCssPath(cssPath, "cssPath"));
        return this;
    }

    /**
     * Replace all custom CSS paths with the provided ordered list.
     */
    public ShellBuilder withCustomCss(List<String> cssPaths) {
        if (cssPaths == null) {
            throw new IllegalArgumentException("cssPaths cannot be null");
        }
        this.customCssPaths.clear();
        for (String cssPath : cssPaths) {
            this.customCssPaths.add(requireCssPath(cssPath, "cssPaths entry"));
        }
        return this;
    }

    /**
     * Append an additional custom CSS file path.
     */
    public ShellBuilder addCustomCss(String cssPath) {
        this.customCssPaths.add(requireCssPath(cssPath, "cssPath"));
        return this;
    }

    /**
     * Replace all custom JS paths with a single JS path.
     */
    public ShellBuilder withCustomJs(String jsPath) {
        this.customJsPaths.clear();
        this.customJsPaths.add(requireJsPath(jsPath, "jsPath"));
        return this;
    }

    /**
     * Replace all custom JS paths with the provided ordered list.
     */
    public ShellBuilder withCustomJs(List<String> jsPaths) {
        if (jsPaths == null) {
            throw new IllegalArgumentException("jsPaths cannot be null");
        }
        this.customJsPaths.clear();
        for (String jsPath : jsPaths) {
            this.customJsPaths.add(requireJsPath(jsPath, "jsPaths entry"));
        }
        return this;
    }

    /**
     * Append an additional custom JS file path.
     */
    public ShellBuilder addCustomJs(String jsPath) {
        this.customJsPaths.add(requireJsPath(jsPath, "jsPath"));
        return this;
    }

    /**
     * Enable/disable framework CSS inclusion.
     */
    public ShellBuilder withFrameworkCss(boolean include) {
        this.includeFrameworkCss = include;
        return this;
    }

    /**
     * Set framework CSS path used when framework CSS is enabled.
     */
    public ShellBuilder withFrameworkCssPath(String cssPath) {
        this.frameworkCssPath = requireCssPath(cssPath, "cssPath");
        return this;
    }

    /**
     * Builds a full HTML document string including doctype.
     */
    public String build() {
        HtmlTag html = new HtmlTag("html").withAttribute("lang", "en");

        HtmlTag head = new HtmlTag("head")
            .withChild(new HtmlTag("meta", true).withAttribute("charset", "UTF-8"))
            .withChild(new HtmlTag("meta", true).withAttribute("name", "viewport")
                .withAttribute("content", "width=device-width, initial-scale=1.0"))
            .withChild(new HtmlTag("title").withInnerText(pageTitle));

        if (includeFrameworkCss) {
            head.withChild(new HtmlTag("link", true).withAttribute("rel", "stylesheet")
                .withAttribute("href", frameworkCssPath));
        }

        for (String customCssPath : customCssPaths) {
            head.withChild(new HtmlTag("link", true).withAttribute("rel", "stylesheet")
                .withAttribute("href", customCssPath));
        }

        if (includeHtmx) {
            head.withChild(new HtmlTag("script")
                .withAttribute("src", "/webjars/htmx.org/dist/htmx.min.js")
                .withAttribute("defer", ""));
        }
        head.withChild(new HtmlTag("script")
            .withAttribute("src", "/js/framework.js")
            .withAttribute("defer", ""));
        for (String customJsPath : customJsPaths) {
            head.withChild(new HtmlTag("script")
                .withAttribute("src", customJsPath)
                .withAttribute("defer", ""));
        }

        HtmlTag body = buildShellContent();
        appendInlineScripts(body);

        html.withChild(head).withChild(body);
        return "<!DOCTYPE html>\n" + html.render();
    }

    /**
     * Builds shell body content only, without {@code html/head} tags.
     */
    public Component buildBody() {
        HtmlTag body = new HtmlTag("div")
            .withAttribute("class", "shell-body");

        if (sideNav != null) {
            body.withChild(buildMobileSidebarToggle(false));
        }

        body.withChild(buildMainContainer(false));
        return body;
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

        if (sideNav != null) {
            body.withChild(buildMobileSidebarToggle(true));
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
        if (contentTargetClass != null) {
            contentArea.withAttribute("class", contentTargetClass);
        }
        if (content != null) {
            contentArea.withChild(content);
        } else if (includeHtmx) {
            contentArea.withAttribute("hx-get", "/home")
                .withAttribute("hx-trigger", "load");
        }

        Component wrappedContentArea = contentWrapper.apply(contentArea);
        if (wrappedContentArea == null) {
            throw new IllegalArgumentException("wrapper cannot return null");
        }
        mainContainer.withChild(new HtmlTag("main")
            .withAttribute("class", "content-wrapper")
            .withChild(wrappedContentArea));
        return mainContainer;
    }

    private HtmlTag buildMobileSidebarToggle(boolean includeAriaLabel) {
        HtmlTag mobileToggle = new HtmlTag("button")
            .withAttribute("type", "button")
            .withAttribute("class", "mobile-sidebar-toggle")
            .withAttribute("onclick", "toggleMobileSidebar()")
            .withAttribute("aria-controls", "main-sidebar")
            .withAttribute("aria-expanded", "false")
            .withInnerText("☰");

        if (includeAriaLabel) {
            mobileToggle.withAttribute("aria-label", "Toggle navigation");
        }

        return mobileToggle;
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

    private String requireCssPath(String cssPath, String fieldName) {
        return requireNonBlank(cssPath, fieldName);
    }

    private String requireJsPath(String jsPath, String fieldName) {
        return requireNonBlank(jsPath, fieldName);
    }

    private String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or blank");
        }
        return value;
    }
}
