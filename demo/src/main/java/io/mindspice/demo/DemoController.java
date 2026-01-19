package io.mindspice.demo;

import io.mindspice.demo.pages.*;
import io.mindspice.simplypages.builders.AccountBarBuilder;
import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.SideNavBuilder;
import io.mindspice.simplypages.builders.TopBannerBuilder;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.components.forum.ForumPost;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;
import java.util.Optional;
import java.util.TreeMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Main demo controller for the Java HTML Framework.
 *
 * <p>This controller serves as the routing layer, delegating page rendering
 * to dedicated page classes. It demonstrates clean separation of concerns:
 * the controller handles Spring MVC routing while page classes handle
 * content generation.</p>
 *
 * <h2>Architecture</h2>
 * <ul>
 *   <li><strong>Controller:</strong> Manages endpoints and HTTP concerns</li>
 *   <li><strong>Page Classes:</strong> Generate HTML content using JHF</li>
 *   <li><strong>Framework:</strong> Pure component library with no web dependencies</li>
 * </ul>
 *
 * <h2>Demo Pages</h2>
 * <ul>
 *   <li><strong>/home:</strong> Framework overview and introduction</li>
 *   <li><strong>/components:</strong> Basic components demonstration</li>
 *   <li><strong>/forms:</strong> Form components and validation</li>
 *   <li><strong>/display:</strong> Data display components</li>
 *   <li><strong>/tables:</strong> Tables and DataTables</li>
 *   <li><strong>/navigation:</strong> Navigation components</li>
 *   <li><strong>/gallery:</strong> Media and gallery components</li>
 *   <li><strong>/forum:</strong> Forum and discussion components</li>
 *   <li><strong>/cards:</strong> Cards and info boxes</li>
 *   <li><strong>/alerts:</strong> Alerts, badges, and tags</li>
 *   <li><strong>/modules:</strong> Module system examples</li>
 *   <li><strong>/layouts:</strong> Layout system (rows, columns, grids)</li>
 *   <li><strong>/module-layouts:</strong> Complex module layouts</li>
 *   <li><strong>/htmx:</strong> HTMX integration patterns</li>
 *   <li><strong>/custom:</strong> Creating custom components</li>
 *   <li><strong>/shell-demo:</strong> Shell builder demonstration</li>
 *   <li><strong>/new-components:</strong> Newly added components (Spacer, Divider, etc.)</li>
 *   <li><strong>/new-modules:</strong> Newly added modules (Hero, Stats, Timeline, etc.)</li>
 *   <li><strong>/dynamic-updates:</strong> Dynamic updates via Templates and HTMX</li>
 * </ul>
 */
@Controller
public class DemoController {

    // Inject all page classes via Spring
    private final HomePage homePage;
    private final ComponentsPage componentsPage;
    private final FormsPage formsPage;
    private final DisplayPage displayPage;
    private final TablesPage tablesPage;
    private final NavigationPage navigationPage;
    private final GalleryPage galleryPage;
    private final ForumPage forumPage;
    private final CardsPage cardsPage;
    private final AlertsPage alertsPage;
    private final ModulesPage modulesPage;
    private final LayoutsPage layoutsPage;
    private final ModuleLayoutsPage moduleLayoutsPage;
    private final HtmxPage htmxPage;
    private final CustomPage customPage;
    private final ShellDemoPage shellDemoPage;
    private final NewComponentsPage newComponentsPage;
    private final NewModulesPage newModulesPage;
    private final PageLayoutsPage pageLayoutsPage;
    private final ScrollingDemoPage scrollingDemoPage;
    private final StickySidebarDemoPage stickySidebarDemoPage;
    private final DynamicUpdatesPage dynamicUpdatesPage;
    private final AdvancedRenderingPage advancedRenderingPage;
    private final JavadocsPage javadocsPage;

    @Autowired
    public DemoController(
            HomePage homePage,
            ComponentsPage componentsPage,
            FormsPage formsPage,
            DisplayPage displayPage,
            TablesPage tablesPage,
            NavigationPage navigationPage,
            GalleryPage galleryPage,
            ForumPage forumPage,
            CardsPage cardsPage,
            AlertsPage alertsPage,
            ModulesPage modulesPage,
            LayoutsPage layoutsPage,
            ModuleLayoutsPage moduleLayoutsPage,
            HtmxPage htmxPage,
            CustomPage customPage,
            ShellDemoPage shellDemoPage,
            NewComponentsPage newComponentsPage,
            NewModulesPage newModulesPage,
            PageLayoutsPage pageLayoutsPage,
            ScrollingDemoPage scrollingDemoPage,
            StickySidebarDemoPage stickySidebarDemoPage,
            DynamicUpdatesPage dynamicUpdatesPage,
            AdvancedRenderingPage advancedRenderingPage,
            JavadocsPage javadocsPage
    ) {
        this.homePage = homePage;
        this.componentsPage = componentsPage;
        this.formsPage = formsPage;
        this.displayPage = displayPage;
        this.tablesPage = tablesPage;
        this.navigationPage = navigationPage;
        this.galleryPage = galleryPage;
        this.forumPage = forumPage;
        this.cardsPage = cardsPage;
        this.alertsPage = alertsPage;
        this.modulesPage = modulesPage;
        this.layoutsPage = layoutsPage;
        this.moduleLayoutsPage = moduleLayoutsPage;
        this.htmxPage = htmxPage;
        this.customPage = customPage;
        this.shellDemoPage = shellDemoPage;
        this.newComponentsPage = newComponentsPage;
        this.newModulesPage = newModulesPage;
        this.pageLayoutsPage = pageLayoutsPage;
        this.scrollingDemoPage = scrollingDemoPage;
        this.stickySidebarDemoPage = stickySidebarDemoPage;
        this.dynamicUpdatesPage = dynamicUpdatesPage;
        this.advancedRenderingPage = advancedRenderingPage;
        this.javadocsPage = javadocsPage;
    }

    /**
     * Helper method to render pages with shell when needed.
     *
     * <p>Checks if the request is from HTMX or direct browser navigation:
     * <ul>
     *   <li>HTMX request (HX-Request header present): Returns partial HTML only</li>
     *   <li>Direct navigation (no header): Returns full shell with page content</li>
     * </ul>
     *
     * <p>Sets the Vary header to ensure CDNs/proxies cache both versions separately.</p>
     *
     * @param hxRequest the HX-Request header value (null if not present)
     * @param page the page to render
     * @param response the HTTP response for setting headers
     * @return HTML string (partial or full shell)
     */
    private String renderWithShellIfNeeded(
            String hxRequest,
            DemoPage page,
            HttpServletResponse response
    ) {
        // Tell caches to store separate versions based on HX-Request header
        response.setHeader("Vary", "HX-Request");

        // If this is an HTMX request, return just the page content
        if (hxRequest != null) {
            return page.render();
        }

        // For direct navigation, build full shell with page content
        String shell = ShellBuilder.create()
                .withPageTitle("Java HTML Framework - Demo")
                .withTopBanner(
                        TopBannerBuilder.create()
                                .withTitle("Java HTML Framework")
                                .withSubtitle("Build server-side rendered web apps with pure Java • Minimal JavaScript • Type-safe components")
                                .withBackgroundColor("#2c3e50")
                                .withTextColor("#ffffff")
                                .withClass("banner-full-width")
                                .build()
                )
                .withAccountBar(
                        AccountBarBuilder.create()
                                .addLeftLink("Home", "/home")
                                .addLeftLink("Docs", "/docs/getting-started/01-introduction")
                                .addLeftLink("Javadocs", "/javadocs-view")
                                .addRightAccountWidget("/api/account-status")
                                .build()
                )
                .withSideNav(
                        SideNavBuilder.create()
                                .addSection("Getting Started")
                                .addLink("Home", "/home", false, "🏠")
                                .addSection("Core Components")
                                .addLink("Components", "/components", "🧱")
                                .addLink("Forms", "/forms", "📝")
                                .addLink("Tables", "/tables", "📊")
                                .addLink("Display", "/display", "🎨")
                                .addLink("Navigation", "/navigation", "🧭")
                                .addLink("Gallery", "/gallery", "🖼️")
                                .addLink("Forum", "/forum", "💬")
                                .addLink("Cards", "/cards", "🃏")
                                .addLink("Alerts", "/alerts", "⚠️")
                                .addSection("New Components")
                                .addLink("New Components", "/new-components", "✨")
                                .addSection("Modules")
                                .addLink("Modules", "/modules", "📦")
                                .addLink("Module Layouts", "/module-layouts", "🏗️")
                                .addLink("New Modules", "/new-modules", "🆕")
                                .addSection("Advanced")
                                .addLink("Layouts", "/layouts", "📐")
                                .addLink("Page Layouts", "/page-layouts", "📄")
                                .addLink("HTMX", "/htmx", "⚡")
                                .addLink("Dynamic Updates", "/demo/dynamic-updates", "🔄")
                                .addLink("Editing System", "/editing-demo", "✏️")
                                .addLink("Advanced Rendering", "/advanced-rendering", "🚀")
                                .addLink("Custom", "/custom", "🔧")
                                .addLink("Shell Demo", "/shell-demo", "🐚")
                                .build()
                )
                .build();

        // Remove auto-loading attributes from content area to prevent auto-fetch of /home
        // This allows us to inject content directly without it being overwritten
        shell = shell.replace(
                "<div id=\"content-area\" hx-get=\"/home\" hx-trigger=\"load\">",
                "<div id=\"content-area\">"
        );

        // Inject page content into the shell's content area
        return shell.replace("<div id=\"content-area\"></div>",
                "<div id=\"content-area\">" + page.render() + "</div>");
    }

    /**
     * Main shell - generates the complete application shell with navigation.
     * Uses ShellBuilder to programmatically create the shell structure.
     * Individual pages are loaded into the content area via HTMX.
     *
     * When accessed via HTMX (e.g., back button), returns home content only.
     * Sets Vary header to ensure proper caching by CDNs and proxies.
     */
    @GetMapping("/")
    @ResponseBody
    public String mainShell(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, homePage, response);
    }

    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private final Map<String, String> docsCache = new ConcurrentHashMap<>();
    private Map<String, List<String>> cachedDocsStructure = null;

    @GetMapping(value = {"/docs/**", "/docs"})
    @ResponseBody
    public String docs(
            HttpServletResponse response,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            jakarta.servlet.http.HttpServletRequest request
    ) {
        String requestUri = request.getRequestURI();
        // Handle /docs or /docs/ safely
        String path = "";
        if (requestUri.length() > "/docs/".length()) {
             path = requestUri.substring("/docs/".length());
        }

        // Normalize path: handle empty and default to introduction
        if (path.isEmpty() || path.equals("/")) {
            path = "getting-started/01-introduction.md";
        }

        // Support both with and without .md
        // We will store in cache using the provided path, but load logic needs to be smart
        String markdown = docsCache.computeIfAbsent(path, this::loadDocContent);

        if (markdown == null) {
            response.setStatus(404);
            return "Documentation not found: " + path;
        }

        String title = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        // Basic title formatting
        title = title.replace("-", " ").replace(".md", "");
        title = title.substring(0, 1).toUpperCase() + title.substring(1);

        Component sidebar = getDocsNavigation();
        DocsPage docsPage = new DocsPage(title, markdown, sidebar);

        // Tell caches to store separate versions based on HX-Request header
        response.setHeader("Vary", "HX-Request");

        // If this is an HTMX request from the sidebar navigation, return just the content
        if (hxRequest != null) {
            return docsPage.renderContent();
        }

        // For direct navigation, build full shell with page content
        return renderWithShellIfNeeded(null, docsPage, response);
    }

    private String loadDocContent(String path) {
        if (path.contains("..")) {
            return null; // Prevent path traversal
        }

        // If path ends with .md, use it as is for lookup, otherwise append .md
        String lookupPath = path.endsWith(".md") ? path : path + ".md";
        String cleanPath = path.endsWith(".md") ? path.substring(0, path.length() - 3) : path;

        try {
            // 1. Try direct match
            Resource resource = resourceResolver.getResource("classpath:static/docs/" + lookupPath);
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }

            // 2. Recursive search for file name
            String fileName = lookupPath.contains("/") ? lookupPath.substring(lookupPath.lastIndexOf('/') + 1) : lookupPath;
            Resource[] resources = resourceResolver.getResources("classpath:static/docs/**/" + fileName);
            if (resources.length > 0) {
                 return new String(resources[0].getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Component getDocsNavigation() {
        Map<String, List<String>> structure = getDocsStructure();
        Div navContainer = new Div().withClass("docs-nav");

        // Helper to add links with HTMX attributes for in-place content updates
        java.util.function.BiConsumer<Div, String> addLink = (container, filePath) -> {
            String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf('/') + 1) : filePath;
            String title = fileName.replace(".md", "").replace("-", " ");
            if (title.matches("^\\d+\\s.*")) {
                title = title.replaceAll("^\\d+\\s", "");
            }
            title = title.substring(0, 1).toUpperCase() + title.substring(1);

            container.withChild(
                new Div().withClass("mb-1").withChild(
                    new Link("/docs/" + filePath, title)
                        .withClass("text-decoration-none text-dark")
                        .withHxGet("/docs/" + filePath)
                        .withHxTarget("#docs-content")
                        .withHxSwap("innerHTML scroll:top")
                        .withHxPushUrl(true)
                )
            );
        };

        // Build navigation from cached structure
        if (structure.containsKey("Getting Started")) {
            navContainer.withChild(Header.H4("Getting Started").withClass("mb-2 mt-3"));
            structure.get("Getting Started").stream().sorted().forEach(file -> addLink.accept(navContainer, file));
        }

        structure.forEach((section, files) -> {
            if (!section.equals("Getting Started")) {
                navContainer.withChild(Header.H4(section).withClass("mb-2 mt-4"));
                files.stream().sorted().forEach(file -> addLink.accept(navContainer, file));
            }
        });

        return navContainer;
    }

    private synchronized Map<String, List<String>> getDocsStructure() {
        if (cachedDocsStructure != null) {
            return cachedDocsStructure;
        }

        try {
            Resource[] resources = resourceResolver.getResources("classpath:static/docs/**/*.md");

            // Group by parent directory name
            Map<String, List<String>> sections = new TreeMap<>();

            for (Resource res : resources) {
                String relativePath = res.getURL().toString();
                // Extract part after /docs/
                if (relativePath.contains("/docs/")) {
                    String part = relativePath.substring(relativePath.indexOf("/docs/") + 6);
                    String folder = "General";
                    if (part.contains("/")) {
                        folder = part.substring(0, part.lastIndexOf('/'));
                        // Capitalize and format folder name
                        folder = Arrays.stream(folder.split("-"))
                            .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                            .collect(Collectors.joining(" "));
                    }
                    sections.computeIfAbsent(folder, k -> new ArrayList<>()).add(part);
                }
            }

            cachedDocsStructure = sections;
            return cachedDocsStructure;

        } catch (IOException e) {
            e.printStackTrace();
            return new TreeMap<>();
        }
    }


    @GetMapping("/home")
    @ResponseBody
    public String home(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, homePage, response);
    }

    @GetMapping("/components")
    @ResponseBody
    public String components(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, componentsPage, response);
    }

    @GetMapping("/forms")
    @ResponseBody
    public String forms(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, formsPage, response);
    }

    @GetMapping("/display")
    @ResponseBody
    public String display(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, displayPage, response);
    }

    @GetMapping("/tables")
    @ResponseBody
    public String tables(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, tablesPage, response);
    }

    @GetMapping("/navigation")
    @ResponseBody
    public String navigation(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, navigationPage, response);
    }

    @GetMapping("/gallery")
    @ResponseBody
    public String gallery(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, galleryPage, response);
    }

    @GetMapping("/forum")
    @ResponseBody
    public String forum(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, forumPage, response);
    }

    @GetMapping("/cards")
    @ResponseBody
    public String cards(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, cardsPage, response);
    }

    @GetMapping("/alerts")
    @ResponseBody
    public String alerts(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, alertsPage, response);
    }

    @GetMapping("/modules")
    @ResponseBody
    public String modules(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, modulesPage, response);
    }

    @GetMapping("/layouts")
    @ResponseBody
    public String layouts(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, layoutsPage, response);
    }

    @GetMapping("/module-layouts")
    @ResponseBody
    public String moduleLayouts(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, moduleLayoutsPage, response);
    }

    @GetMapping("/htmx")
    @ResponseBody
    public String htmx(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, htmxPage, response);
    }

    @GetMapping("/demo/dynamic-updates")
    @ResponseBody
    public String dynamicUpdates(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, dynamicUpdatesPage, response);
    }

    @GetMapping("/javadocs-view")
    @ResponseBody
    public String javadocsView(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, javadocsPage, response);
    }

    @GetMapping("/advanced-rendering")
    @ResponseBody
    public String advancedRendering(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, advancedRenderingPage, response);
    }

    @GetMapping("/custom")
    @ResponseBody
    public String custom(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, customPage, response);
    }

    @GetMapping("/shell-demo")
    @ResponseBody
    public String shellDemo(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, shellDemoPage, response);
    }

    @GetMapping("/new-components")
    @ResponseBody
    public String newComponents(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, newComponentsPage, response);
    }

    @GetMapping("/new-modules")
    @ResponseBody
    public String newModules(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, newModulesPage, response);
    }

    @GetMapping("/page-layouts")
    @ResponseBody
    public String pageLayouts(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, pageLayoutsPage, response);
    }

    @GetMapping("/page-layouts/scrolling-demo")
    @ResponseBody
    public String scrollingDemo(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, scrollingDemoPage, response);
    }

    @GetMapping("/page-layouts/sticky-demo")
    @ResponseBody
    public String stickyDemo(
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            HttpServletResponse response
    ) {
        return renderWithShellIfNeeded(hxRequest, stickySidebarDemoPage, response);
    }

    // HTMX API endpoints (simple responses for demo purposes)
    @GetMapping("/api/more-items")
    @ResponseBody
    public String moreItems() {
        return "<div class='item'>Item 4</div><div class='item'>Item 5</div><div class='item'>Item 6</div>";
    }

    @PostMapping("/api/save")
    @ResponseBody
    public String saveData() {
        return "<div class='alert alert-success'>Data saved successfully!</div>";
    }

    @GetMapping("/api/edit-form")
    @ResponseBody
    public String editForm() {
        return "<form><input type='text' name='name' value='John Doe'><button type='submit'>Save</button></form>";
    }

    @GetMapping("/api/data")
    @ResponseBody
    public String publicData() {
        return "<div>Public data loaded</div>";
    }

    @GetMapping("/api/admin/data")
    @ResponseBody
    public String adminData() {
        return "<div>Admin data loaded</div>";
    }

    /**
     * HTMX endpoint for account widget.
     * Returns account widget HTML based on authentication status.
     * For demo purposes, uses session to maintain consistent authentication state.
     */
    @GetMapping("/api/account-status")
    @ResponseBody
    public String accountStatus(HttpSession session) {
        // For demo: use session to maintain consistent authentication state
        // Initialize on first access if not present
        Boolean isAuthenticated = (Boolean) session.getAttribute("isAuthenticated");
        if (isAuthenticated == null) {
            // First time - randomly set and store in session
            isAuthenticated = Math.random() > 0.5;
            session.setAttribute("isAuthenticated", isAuthenticated);
        }

        if (isAuthenticated) {
            return io.mindspice.simplypages.components.AccountWidget
                .createAuthenticated("demo_user")
                .withProfileUrl("/profile")
                .withLogoutUrl("/logout")
                .render();
        } else {
            return io.mindspice.simplypages.components.AccountWidget
                .createGuest()
                .withLoginUrl("/login")
                .withSignupUrl("/signup")
                .render();
        }
    }

    @GetMapping("/custom-shell")
    @ResponseBody
    public String customShell() {
        // Return a simple custom shell demo
        return ShellBuilder.create()
                .withPageTitle("Custom Shell Demo")
                .withTopBanner(
                        TopBannerBuilder.create()
                                .withTitle("Custom Shell")
                                .build()
                )
                .build();
    }

    // --- Dynamic Updates Demo Endpoints ---

    @PostMapping("/demo/dynamic-updates/update-module")
    @ResponseBody
    public String updateDynamicModule(
            @RequestParam("target") String target,
            @RequestParam("val1") String val1,
            @RequestParam("val2") String val2,
            @RequestParam("val3") String val3
    ) {
        // Depending on target, we return the OOB swapped content for that specific module.
        // We use the helper methods in DynamicUpdatesPage which use Templates.
        // Since we removed hx-swap-oob="true" from the templates to fix initial rendering,
        // we must now inject it manually for the OOB updates.
        String html;
        String moduleId;

        switch (target) {
            case "card":
                html = DynamicUpdatesPage.renderCard(val1, val2);
                moduleId = "card-module";
                break;
            case "list":
                html = DynamicUpdatesPage.renderList(List.of(val1, val2, val3));
                moduleId = "list-module";
                break;
            case "table":
                html = DynamicUpdatesPage.renderTable(val1, val2, val3);
                moduleId = "table-module";
                break;
            default:
                return "<div id='error'>Invalid target</div>";
        }

        // Inject hx-swap-oob="true" into the top-level element
        // The top level element will have the ID we set in the template
        return html.replace("id=\"" + moduleId + "\"", "id=\"" + moduleId + "\" hx-swap-oob=\"true\"");
    }

    @PostMapping("/demo/dynamic-updates/add-post")
    @ResponseBody
    public String addForumPost(
            @RequestParam("content") String content,
            HttpSession session
    ) {
        // Retrieve or initialize posts from session
        @SuppressWarnings("unchecked")
        List<ForumPost> posts = (List<ForumPost>) session.getAttribute("forum_posts");
        if (posts == null) {
            posts = DynamicUpdatesPage.getInitialPosts();
        }

        // Add new post
        posts.add(ForumPost.create()
                .withAuthor("DemoUser")
                .withTimestamp("Just now")
                .withTitle("User Post")
                .withContent(content)
                .withLikes(0)
        );

        session.setAttribute("forum_posts", posts);

        // Return the rendered forum module (replacing the old one)
        // Since we are replacing the target, we don't strictly need OOB,
        // but if we used OOB for other parts (like resetting input) we might mix them.
        // Here we just return the module HTML, which replaces #forum-module.
        // The form reset is handled by client-side JS (hx-on::after-request).
        return DynamicUpdatesPage.renderForumModule(posts).render(null);
    }

    // --- Advanced Rendering Demo Endpoints ---

    @GetMapping("/demo/advanced/layout")
    @ResponseBody
    public String getAdvancedLayout(@RequestParam("complex") boolean complex) {
        return advancedRenderingPage.renderPatternAInner(complex).render(null);
    }

    // Wiki demo session state
    private static final String WIKI_CONTENT_SESSION_KEY = "wiki_content_v1";

    @GetMapping("/demo/wiki/display")
    @ResponseBody
    public String wikiDisplay(HttpSession session) {
        String content = (String) session.getAttribute(WIKI_CONTENT_SESSION_KEY);
        if (content == null) {
            content = "This is a wiki article. Click edit to modify me.";
            session.setAttribute(WIKI_CONTENT_SESSION_KEY, content);
        }
        return AdvancedRenderingPage.renderWikiDisplay(content).render(null);
    }

    @GetMapping("/demo/wiki/edit")
    @ResponseBody
    public String wikiEdit(HttpSession session) {
        String content = (String) session.getAttribute(WIKI_CONTENT_SESSION_KEY);
        if (content == null) {
            content = "This is a wiki article. Click edit to modify me.";
            session.setAttribute(WIKI_CONTENT_SESSION_KEY, content);
        }
        return AdvancedRenderingPage.renderWikiEdit(content).render(null);
    }

    @PostMapping("/demo/wiki/save")
    @ResponseBody
    public String wikiSave(@RequestParam("content") String content, HttpSession session) {
        session.setAttribute(WIKI_CONTENT_SESSION_KEY, content);
        return AdvancedRenderingPage.renderWikiDisplay(content).render(null);
    }
}
