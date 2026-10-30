package io.mindspice.demo.pages;

import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.HtmlTag;
import java.util.List;

/** Site-owned taxonomy. All links are ordinary document navigation, including chat. */
public final class DemoNavigation {
    public record Category(String section, String title, String route, String description, List<String> members) {}
    private static Category category(String section, String title, String route, String description, String... members) {
        return new Category(section, title, route, description, List.of(members));
    }
    private static final List<Category> CATEGORIES = List.of(
        category("Components", "Text & structure", "/demos/components/text-structure", "Headings, prose, Markdown and the building blocks of a document."),
        category("Components", "Forms & input", "/demos/components/forms", "Native fields, validation and real server-rendered form results."),
        category("Components", "Display & data", "/demos/components/display-data", "Cards, tables, status indicators and accessible dialogs."),
        category("Components", "Navigation", "/demos/components/navigation", "Links, breadcrumbs, menus and account displays."),
        category("Components", "Media", "/demos/components/media", "Local images, galleries, audio and video."),
        category("Components", "Page layout", "/demos/layout", "Compose responsive pages with rows, columns and grids."),
        category("Composed modules", "Reading & content", "/demos/modules/reading", "Articles, quotations, lists and a content timeline.", "content", "rich-content", "quote", "simple-list", "timeline", "dynamic-list"),
        category("Composed modules", "Data & insights", "/demos/modules/insights", "Typed tables, comparisons, metrics and request-owned data.", "data", "stats", "comparison", "dynamic-table"),
        category("Composed modules", "Interaction & feedback", "/demos/modules/interaction", "Accordions, tabs, forms, notices and an updatable card.", "accordion", "tabs", "callout", "form", "dynamic-card"),
        category("Composed modules", "Storytelling & media", "/demos/modules/storytelling", "A strong introduction and a gallery of local studies.", "hero", "gallery"),
        category("Composed modules", "Shells & builders", "/demos/builders/composition", "Compose the banner, navigation and reusable document shell.", "banner", "top-nav", "side-nav", "account-bar", "shell", "shell-template"),
        category("Working features", "Typed templates", "/demos/features/templates", "Reusable structure with typed values supplied per request."),
        category("Working features", "HTMX & fragments", "/demos/features/htmx", "Forms and partial updates driven by real HTML responses."),
        category("Working features", "Editing & proposals", "/demos/features/editing", "Try nested editing, validation and approval in your session."),
        category("Working features", "Markdown publishing", "/demos/features/static-content", "A local publication built from packaged Markdown."),
        category("Working features", "Forum & discussions", "/demos/features/forum", "Topics, comments and tagged content in your own sandbox."),
        category("Working features", "Chat & finite SSE", "/demos/features/chat", "A real conversation with explicit stream recovery."),
        category("Working features", "Safe rendering", "/demos/features/safe-rendering", "Escaping, URL safety and application-owned permission checks.")
    );
    private DemoNavigation() {}
    public static List<Category> categories() { return CATEGORIES; }
    public static Category current(String path) {
        return CATEGORIES.stream().filter(c -> c.route().equals(path) || c.members().stream()
            .anyMatch(m -> path.equals(c.route().substring(0, c.route().lastIndexOf('/') + 1) + m)))
            .findFirst().orElse(null);
    }
    public static List<SiteCatalog.Example> examples(Category category) {
        if (category == null) return List.of();
        return SiteCatalog.examples().stream().filter(e -> current(e.route()) == category)
            .sorted(java.util.Comparator.comparingInt(e -> category.members().indexOf(e.route().substring(e.route().lastIndexOf('/') + 1))))
            .toList();
    }
    public static HtmlTag sidebar(String path) {
        Category current = current(path);
        HtmlTag nav = new HtmlTag("nav").withId("sp-demo-links").withAttribute("aria-label", "Demo navigation");
        Link overview = Link.create("/demos", "Explore the demos").withClass("sp-site-demo-overview");
        if (path.equals("/demos")) overview.withAttribute("aria-current", "page");
        nav.withChild(overview);
        for (String section : List.of("Components", "Composed modules", "Working features")) {
            HtmlTag group = new HtmlTag("div").withClass("sp-site-nav-group")
                .withChild(new Paragraph(section).withClass("sp-site-nav-label"));
            for (Category category : CATEGORIES.stream().filter(c -> c.section().equals(section)).toList()) {
                Link link = Link.create(category.route(), category.title());
                if (category == current) link.withAttribute("aria-current", "page");
                group.withChild(link);
                if (category == current) {
                    HtmlTag sections = new HtmlTag("div").withClass("sp-site-nav-sections");
                    List<SiteCatalog.Example> examples = examples(category).stream()
                        .filter(e -> path.equals(category.route()) || e.route().equals(path)).toList();
                    examples.forEach(e -> sections.withChild(Link.create(category.route() + "#" + e.id(), e.symbol().getSimpleName())));
                    String feature = path.substring(path.lastIndexOf('/') + 1);
                    FeatureCatalog.contracts().stream().filter(c -> c.feature().equals(feature.equals("htmx") ? "templates" : feature))
                        .map(FeatureCatalog.Contract::anchor).distinct()
                        .filter(anchor -> examples.stream().noneMatch(e -> e.id().equals(anchor)))
                        .forEach(anchor -> sections.withChild(Link.create(category.route() + "#" + anchor, sectionLabel(anchor))));
                    group.withChild(sections);
                }
            }
            nav.withChild(group);
        }
        nav.withChild(Link.create("/demos/recovery", "Sandbox recovery").withClass("sp-site-demo-recovery"));
        HtmlTag details = new HtmlTag("details").withClass("sp-site-demo-disclosure").withAttribute("open", "open")
            .withChild(new HtmlTag("summary").withAttribute("aria-controls", "sp-demo-links")
                .withInnerText("Browse demos" + (current == null ? "" : " · " + current.title())))
            .withChild(nav);
        return new HtmlTag("aside").withClass("sp-site-demo-sidebar").withChild(details);
    }
    private static String sectionLabel(String anchor) {
        return switch (anchor) {
            case "typed-slots" -> "Typed slots";
            case "module-entries" -> "Module entries";
            case "bound-context" -> "Bound context";
            case "edit-form-builder" -> "Fields & validation";
            case "editablepage" -> "Page & row wrappers";
            case "live-editing-contracts" -> "Live editing";
            case "module-edit-handler" -> "Handler preview";
            case "authwrapper" -> "Permission checks";
            case "content-pipeline" -> "Content pipeline";
            case "chatmodule" -> "Live conversation";
            case "forum-main" -> "Live discussion";
            case "default-category" -> "Category renderers";
            case "url-style-safety" -> "URL & style safety";
            case "style-composition" -> "Style composition";
            default -> anchor;
        };
    }
    public static HtmlTag cards(String section) {
        HtmlTag grid = new HtmlTag("div").withClass("sp-site-category-grid");
        for (Category category : CATEGORIES.stream().filter(c -> c.section().equals(section)).toList()) {
            grid.withChild(Link.create(category.route(), "").withClass("sp-site-category-card")
                .withChild(new Paragraph(category.section()).withClass("sp-site-eyebrow"))
                .withChild(Header.H3(category.title()))
                .withChild(new Paragraph(category.description()))
                .withChild(new HtmlTag("span").withClass("sp-site-card-action").withInnerText("Explore examples →")));
        }
        return grid;
    }
}
