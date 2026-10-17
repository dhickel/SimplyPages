package io.mindspice.demo;

import io.mindspice.demo.pages.*;
import io.mindspice.simplypages.builders.BannerBuilder;
import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.SideNavBuilder;
import io.mindspice.simplypages.builders.TopNavBuilder;
import io.mindspice.simplypages.components.RawHtml;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.core.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DemoController {

    private final HomePage homePage;
    private final DemosOverviewPage demosOverviewPage;
    private final BasicsFormsDemoPage basicsFormsDemoPage;
    private final DisplayDataDemoPage displayDataDemoPage;
    private final ModulesDemoPage modulesDemoPage;
    private final HtmxEditingDemoPage htmxEditingDemoPage;
    private final JavadocsPage javadocsPage;
    private final DocumentationService documentationService;

    public DemoController(
        HomePage homePage,
        DemosOverviewPage demosOverviewPage,
        BasicsFormsDemoPage basicsFormsDemoPage,
        DisplayDataDemoPage displayDataDemoPage,
        ModulesDemoPage modulesDemoPage,
        HtmxEditingDemoPage htmxEditingDemoPage,
        JavadocsPage javadocsPage,
        DocumentationService documentationService
    ) {
        this.homePage = homePage;
        this.demosOverviewPage = demosOverviewPage;
        this.basicsFormsDemoPage = basicsFormsDemoPage;
        this.displayDataDemoPage = displayDataDemoPage;
        this.modulesDemoPage = modulesDemoPage;
        this.htmxEditingDemoPage = htmxEditingDemoPage;
        this.javadocsPage = javadocsPage;
        this.documentationService = documentationService;
    }

    @GetMapping(value = {"/", "/home"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String home(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInHomeShell(homePage, hxRequest, response);
    }

    @GetMapping(value = "/demos", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String demos(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(demosOverviewPage, hxRequest, response);
    }

    @GetMapping(value = "/demos/basics-forms", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String basicsForms(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(basicsFormsDemoPage, hxRequest, response);
    }

    @GetMapping(value = "/demos/display-data", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String displayData(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(displayDataDemoPage, hxRequest, response);
    }

    @GetMapping(value = "/demos/modules", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String modules(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(modulesDemoPage, hxRequest, response);
    }

    @GetMapping(value = "/demos/htmx-editing", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String htmxEditing(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(htmxEditingDemoPage, hxRequest, response);
    }

    @GetMapping(value = {"/docs/**", "/docs"}, produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String docs(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        addHxVary(response);
        String path = documentationService.normalizePath(request.getRequestURI());
        String markdown = documentationService.getDocContent(path);

        if (markdown == null) {
            response.setStatus(404);
            return "Documentation not found: " + org.owasp.encoder.Encode.forHtml(path);
        }

        DocsPage docsPage = new DocsPage(documentationService.renderMarkdown(path, markdown), documentationService.getDocsNavigation());
        if (isHxRequest(hxRequest)) {
            return docsPage.renderContent();
        }

        return renderInHomeShell(docsPage, null, response);
    }

    @GetMapping(value = "/javadocs-view", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String javadocs(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInHomeShell(javadocsPage, hxRequest, response);
    }

    @PostMapping(value = "/demos/api/template-card", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String templateCard(
        @RequestParam("title") String title,
        @RequestParam("body") String body
    ) {
        return HtmxEditingDemoPage.renderTemplateCard(title, body);
    }

    @PostMapping(value = "/demos/api/form-preview", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String formPreview(
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "email", required = false) String email,
        @RequestParam(value = "role", required = false) String role
    ) {
        String summary = "Received name=" + (name == null ? "" : name)
            + ", email=" + (email == null ? "" : email)
            + ", role=" + (role == null ? "" : role);

        return new RawHtml(new io.mindspice.simplypages.components.Div()
            .withId("form-preview")
            .withChild(Alert.success(summary))
            .render()).render();
    }

    private String renderInHomeShell(DemoPage page, String hxRequest, HttpServletResponse response) {
        addHxVary(response);
        if (isHxRequest(hxRequest)) {
            return page.render();
        }

        return ShellBuilder.create()
            .withPageTitle("SimplyPages")
            .withTopBanner(BannerBuilder.create()
                .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                .withTitle("SimplyPages")
                .withSubtitle("Java-first server-side rendering framework")
                .build())
            .withTopNav(buildGlobalTopNav())
            .withContent(new RawHtml(page.render()))
            .addCustomJs("/js/editing-demo.js")
            .build();
    }

    private String renderInDemoShell(DemoPage page, String hxRequest, HttpServletResponse response) {
        addHxVary(response);
        if (isHxRequest(hxRequest)) {
            return page.render();
        }

        return ShellBuilder.create()
            .withPageTitle("SimplyPages Demos")
            .withTopBanner(BannerBuilder.create()
                .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
                .withTitle("SimplyPages")
                .withSubtitle("Consolidated demo surface")
                .build())
            .withTopNav(buildGlobalTopNav())
            .withSideNav(SideNavBuilder.create()
                .addSection("Demos")
                .addLink("Overview", "/demos")
                .addLink("Basics & Forms", "/demos/basics-forms")
                .addLink("Display & Data", "/demos/display-data")
                .addLink("Modules", "/demos/modules")
                .addLink("HTMX & Editing", "/demos/htmx-editing")
                .addLink("Chat", "/chat")
                .build())
            .withContent(new RawHtml(page.render()))
            .addCustomJs("/js/editing-demo.js")
            .build();
    }

    private boolean isHxRequest(String hxRequest) {
        return "true".equalsIgnoreCase(hxRequest);
    }

    private void addHxVary(HttpServletResponse response) {
        for (String header : response.getHeaders("Vary")) {
            for (String token : header.split(",")) {
                if ("HX-Request".equalsIgnoreCase(token.trim())) {
                    return;
                }
            }
        }
        response.addHeader("Vary", "HX-Request");
    }

    private Component buildGlobalTopNav() {
        return TopNavBuilder.create()
            .withHtmxNavigation(false)
            .addPrimaryLink("Home", "/home")
            .addPrimaryLink("Demos", "/demos")
            .addPrimaryLink("Javadocs", "/javadocs-view")
            .addPrimaryLink("Forum", "/forum")
            .addPrimaryLink("Chat", "/chat")
            .addPrimaryLink("Blog", "/blog")
            .addPrimaryLink("Docs", "/docs")
            .build();
    }
}
