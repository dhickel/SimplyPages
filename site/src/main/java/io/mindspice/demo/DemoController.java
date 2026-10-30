package io.mindspice.demo;

import io.mindspice.demo.pages.*;
import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.*;
import io.mindspice.simplypages.components.navigation.Link;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class DemoController {
    private final HomePage home;
    private final DocumentationService documentation;
    public DemoController(HomePage home, DocumentationService documentation) {
        this.home = home;
        this.documentation = documentation;
    }

    @GetMapping(value = {"/", "/home"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String home(HttpServletRequest request, HttpServletResponse response) {
        return page("Java-first interfaces", "/", home.render(), request, response);
    }

    @GetMapping(value = {"/demos", "/demos/modules", "/demos/builders"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String directory(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        String category = path.endsWith("modules") ? "Modules" : path.endsWith("builders") ? "Builders" : null;
        String title = category == null ? "Working examples, from primitive to page." : category;
        String content = SiteShell.intro(title, "Browse actual SimplyPages output, matching Java, documentation and exact API references.")
            + (category == null ? new Paragraph().withChild(Link.create("/demos/recovery", "Expired sandbox or admission error? Open explicit recovery →")).render() : "")
            + SiteCatalog.directory(category) + (category == null ? FeatureExamples.directory()
                + "<details class=\"sp-site-source-details\"><summary>Supporting contracts, grouped by working feature</summary>"
                + FeatureCatalog.directory() + "</details>" : "");
        return page(title, path, content, request, response);
    }

    @GetMapping(value = {"/demos/components/{slug}", "/demos/layout", "/demos/modules/{slug}", "/demos/builders/{slug}", "/demos/features/{slug}"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String example(HttpServletRequest request, HttpServletResponse response) {
        String path = request.getRequestURI();
        String content = SiteCatalog.render(path, request.getParameter("persona"));
        String slug = path.substring(path.lastIndexOf('/') + 1);
        if (content == null && path.startsWith("/demos/features/")) {
            content = FeatureExamples.render(slug);
            if (content != null) content += FeatureCatalog.references(slug);
        }
        if (content == null) {
            response.setStatus(404);
            content = SiteShell.intro("Example not found", "This is not a catalog destination. Choose a working example from the demo directory.");
        }
        return page(FeatureExamples.title(slug), path, content, request, response);
    }

    @GetMapping(value = "/demos/recovery", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String recovery(HttpServletRequest request, HttpServletResponse response) {
        String content = SiteShell.intro("Sandbox recovery", "This page does not initialize editing, forum or chat content. Reset is an explicit protected POST; nothing is retried or replayed.")
            + SiteShell.sandboxNotice()
            + new Div().withClass("sp-site-actions")
                .withChild(Link.create("/demos/features/editing", "Return to editing"))
                .withChild(Link.create("/demos/features/forum", "Return to forum"))
                .withChild(Link.create("/demos/features/chat", "Chat: use its own Start New control")).render();
        return page("Sandbox recovery", "/demos/recovery", content, request, response);
    }

    @GetMapping("/demos/modules/editable")
    public String editableModule() { return "redirect:/demos/features/editing#editablemodule"; }
    @GetMapping("/demos/modules/chat")
    public String chatModule() { return "redirect:/demos/features/chat#chatmodule"; }

    @GetMapping(value = "/demos/builders/preview/{kind}", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String shellPreview(@PathVariable String kind, HttpServletResponse response) {
        if (!java.util.Set.of("shell", "shell-template", "banner", "top-nav", "side-nav", "account-bar").contains(kind)) {
            response.setStatus(404);
            return SiteShell.render("Preview not found", "/demos", SiteShell.intro("Preview not found", "Choose a shell from the builder catalog."));
        }
        String title = kind.equals("shell") ? "Local shell preview" : kind.equals("shell-template") ? "Template preview" : kind + " · full document preview";
        Div content = new Div();
        if (!kind.equals("banner")) content.withChild(Header.H1(title));
        content.withChild(new Paragraph(kind.equals("shell-template") ? "Content supplied at render time." : "A complete document."))
            .withChild(Link.create("/demos/builders/" + kind, "Back to the builder example"));
        ShellBuilder shell = ShellBuilder.create().withPageTitle(title);
        if (kind.equals("shell") || kind.equals("shell-template")) {
            shell.withSideNav(io.mindspice.simplypages.builders.SideNavBuilder.create()
                .addSection("Preview navigation").addLink("Chat", "/chat").addLink("Documentation", "/docs").build(), true);
        } else {
            var specimen = SiteCatalog.examples().stream().filter(e -> e.route().equals("/demos/builders/" + kind)).findFirst().orElseThrow().preview().get();
            switch (kind) {
                case "banner" -> shell.withTopBanner(specimen);
                case "top-nav", "account-bar" -> shell.withTopNav(specimen);
                case "side-nav" -> content.withChild(specimen);
                default -> throw new IllegalStateException("Unknown builder preview");
            }
        }
        return kind.equals("shell-template") ? shell.buildTemplate().renderWithContent(content) : shell.withContent(content).build();
    }

    @GetMapping({"/demos/basics-forms", "/demos/display-data", "/demos/htmx-editing"})
    public String legacy(HttpServletRequest request) {
        return "redirect:" + switch (request.getRequestURI()) {
            case "/demos/basics-forms" -> "/demos/components/forms";
            case "/demos/display-data" -> "/demos/components/display-data";
            default -> "/demos/features/htmx";
        };
    }

    @GetMapping(value = {"/docs/**", "/docs"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String docs(HttpServletRequest request, HttpServletResponse response) {
        vary(response);
        String path = documentation.normalizePath(request.getRequestURI());
        String markdown = documentation.getDocContent(path);
        String title = markdown == null ? "Documentation not found" : markdown.lines().filter(line -> line.startsWith("# "))
            .map(line -> line.substring(2).trim()).findFirst().orElse("Documentation");
        if (markdown == null) response.setStatus(404);
        String html = markdown == null ? Header.H1(title).render() + new Paragraph("No document exists at: " + path + ". Use the documentation navigation or complete index.").render()
            : documentation.renderMarkdown(path, markdown);
        DocsPage docs = new DocsPage(html, documentation.getDocsNavigation(path), title, path);
        if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) return docs.renderContent();
        return SiteShell.render(title, "/docs", docs.render());
    }

    @GetMapping(value = {"/api", "/javadocs-view"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String api(HttpServletRequest request, HttpServletResponse response) {
        Div content = new Div().withChild(Header.H1("Java API"))
            .withChild(new Paragraph("The generated reference is the native Javadoc document, with package navigation, inherited members, search and local supporting assets."))
            .withChild(Link.create("/javadocs/index.html", "Open the complete Java API →").withClass("sp-site-primary"))
            .withChild(Header.H2("Start with a package"));
        for (String pkg : new String[]{"core", "components", "layout", "modules", "builders", "editing", "components/content", "components/forum", "components/chat"})
            content.withChild(new Div().withChild(Link.create("/javadocs/io/mindspice/simplypages/" + pkg + "/package-summary.html", pkg.replace('/', '.'))));
        content.withChild(Link.create("/docs/reference/README.md", "Read the guided reference"));
        return page("Java API", "/api", content.render(), request, response);
    }

    @PostMapping(value = "/demos/api/template-card", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String template(@RequestParam String title, @RequestParam String body) {
        return FeatureExamples.card(title, body);
    }
    @PostMapping(value = "/demos/api/form-preview", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String form(@RequestParam(defaultValue = "") String name, @RequestParam(defaultValue = "") String email,
                                    @RequestParam(defaultValue = "") String role) {
        return new Div().withId("form-preview").withAttribute("role", "status")
            .withChild(Alert.success("Received name=" + name + ", email=" + email + ", role=" + role)).render();
    }
    @PostMapping(value = "/demos/api/dynamic", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String dynamic(@RequestParam String kind, @RequestParam String value, HttpServletResponse response) {
        if (!java.util.Set.of("dynamic-card", "dynamic-list", "dynamic-table").contains(kind)) {
            response.setStatus(400); return Alert.danger("Choose a dynamic module from the catalog.").render();
        }
        return SiteCatalog.dynamic(kind, value);
    }
    @PostMapping(value = "/demos/api/safe", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String safe(@RequestParam String value) { return FeatureExamples.safe(value); }
    @PostMapping(value = "/demos/api/edit-fields", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String editFields(@RequestParam(defaultValue = "") String draftTitle,
        @RequestParam(defaultValue = "false") boolean markdown) {
        var validation = draftTitle.isBlank() || draftTitle.length() > 80
            ? io.mindspice.simplypages.editing.ValidationResult.invalid("Use a nonblank title of at most 80 characters.")
            : io.mindspice.simplypages.editing.ValidationResult.valid();
        return FeatureExamples.editFieldsResult(draftTitle, markdown,
            validation.isValid() ? null : validation.getErrorsAsString());
    }
    @PostMapping(value = "/demos/api/module-preview", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String modulePreview(@RequestParam(defaultValue = "edit") String operation,
        @RequestParam(defaultValue = "") String title, @RequestParam(defaultValue = "") String content,
        @RequestParam(defaultValue = "OWNER_EDIT") io.mindspice.simplypages.editing.EditMode mode,
        HttpServletResponse response) {
        if (!java.util.Set.of("edit", "update", "delete").contains(operation) || title.length() > 200 || content.length() > 4096) {
            response.setStatus(400);
            return Alert.danger("Choose a preview operation; limit title to 200 and content to 4096 characters.").render();
        }
        return FeatureExamples.handlerResult(operation, title, content, mode);
    }
    @PostMapping(value = "/demos/api/auth", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String auth(@RequestParam(defaultValue = "false") boolean allow) { return FeatureExamples.auth(allow); }
    @GetMapping(value = "/demos/fragments/time", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String time() { return new Paragraph("Server time: " + java.time.Instant.now()).render(); }
    @GetMapping(value = "/demos/fragments/modal", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody public String modal() {
        return Modal.create().withModalId("catalog-modal").withTitle("A server-rendered dialog")
            .withBody(new Paragraph("Escape closes this dialog.")).render();
    }
    private String page(String title, String path, String content, HttpServletRequest request, HttpServletResponse response) {
        vary(response);
        return "true".equalsIgnoreCase(request.getHeader("HX-Request")) ? content : SiteShell.render(title, path, content);
    }
    private void vary(HttpServletResponse response) {
        for (String header : response.getHeaders("Vary")) for (String token : header.split(","))
            if ("HX-Request".equalsIgnoreCase(token.trim())) return;
        response.addHeader("Vary", "HX-Request");
    }
}
