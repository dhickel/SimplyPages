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
    private final FileExplorerDemoPage fileExplorerDemoPage;
    private final JavadocsPage javadocsPage;
    private final DocumentationService documentationService;

    public DemoController(
        HomePage homePage,
        DemosOverviewPage demosOverviewPage,
        BasicsFormsDemoPage basicsFormsDemoPage,
        DisplayDataDemoPage displayDataDemoPage,
        ModulesDemoPage modulesDemoPage,
        HtmxEditingDemoPage htmxEditingDemoPage,
        FileExplorerDemoPage fileExplorerDemoPage,
        JavadocsPage javadocsPage,
        DocumentationService documentationService
    ) {
        this.homePage = homePage;
        this.demosOverviewPage = demosOverviewPage;
        this.basicsFormsDemoPage = basicsFormsDemoPage;
        this.displayDataDemoPage = displayDataDemoPage;
        this.modulesDemoPage = modulesDemoPage;
        this.htmxEditingDemoPage = htmxEditingDemoPage;
        this.fileExplorerDemoPage = fileExplorerDemoPage;
        this.javadocsPage = javadocsPage;
        this.documentationService = documentationService;
    }

    @GetMapping({"/", "/home"})
    @ResponseBody
    public String home(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInHomeShell(homePage, hxRequest, response);
    }

    @GetMapping("/demos")
    @ResponseBody
    public String demos(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(demosOverviewPage, hxRequest, response);
    }

    @GetMapping("/demos/basics-forms")
    @ResponseBody
    public String basicsForms(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(basicsFormsDemoPage, hxRequest, response);
    }

    @GetMapping("/demos/display-data")
    @ResponseBody
    public String displayData(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(displayDataDemoPage, hxRequest, response);
    }

    @GetMapping("/demos/modules")
    @ResponseBody
    public String modules(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(modulesDemoPage, hxRequest, response);
    }

    @GetMapping("/demos/htmx-editing")
    @ResponseBody
    public String htmxEditing(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(htmxEditingDemoPage, hxRequest, response);
    }

    @GetMapping("/demos/file-explorer")
    @ResponseBody
    public String fileExplorer(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInDemoShell(fileExplorerDemoPage, hxRequest, response);
    }

    @GetMapping("/demos/file-picker")
    @ResponseBody
    public String filePicker(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        response.setHeader("Vary", "HX-Request");
        if (hxRequest != null) {
            return fileExplorerDemoPage.renderPicker();
        }
        return renderInDemoShell(() -> fileExplorerDemoPage.renderPicker(), null, response);
    }

    @GetMapping("/demos/file-explorer/list")
    @ResponseBody
    public String fileExplorerList(@RequestParam(value = "path", required = false) String path) {
        return new io.mindspice.simplypages.components.Div()
            .withId("demo-file-explorer-list")
            .withClass("file-explorer-list-pane")
            .withChild(new io.mindspice.simplypages.components.Paragraph("Explorer list for: " + (path == null ? "/workspace/docs" : path)))
            .render();
    }

    @GetMapping("/demos/file-explorer/inspect")
    @ResponseBody
    public String fileExplorerInspect(@RequestParam(value = "path", required = false) String path) {
        return new io.mindspice.simplypages.components.Div()
            .withId("demo-file-explorer-inspector")
            .withClass("file-explorer-inspector-pane")
            .withChild(io.mindspice.simplypages.components.Header.H4("Inspector"))
            .withChild(new io.mindspice.simplypages.components.Paragraph("Inspecting: " + (path == null ? "" : path)))
            .render();
    }

    @GetMapping("/demos/file-explorer/view")
    @ResponseBody
    public String fileExplorerView(@RequestParam(value = "path", required = false) String path) {
        return new io.mindspice.simplypages.components.Div()
            .withId("demo-file-explorer-viewer")
            .withClass("file-explorer-viewer-pane")
            .withChild(io.mindspice.simplypages.components.Header.H4("Viewer"))
            .withChild(new io.mindspice.simplypages.components.Paragraph("Preview for: " + (path == null ? "" : path)))
            .render();
    }

    @GetMapping("/demos/file-picker/list")
    @ResponseBody
    public String filePickerList(@RequestParam(value = "path", required = false) String path) {
        return new io.mindspice.simplypages.components.Div()
            .withId("demo-file-picker-list")
            .withClass("file-explorer-list-pane")
            .withChild(new io.mindspice.simplypages.components.Paragraph("Picker list for: " + (path == null ? "/workspace" : path)))
            .render();
    }

    @GetMapping("/demos/file-picker/inspect")
    @ResponseBody
    public String filePickerInspect(@RequestParam(value = "path", required = false) String path) {
        return new io.mindspice.simplypages.components.Div()
            .withId("demo-file-picker-inspector")
            .withClass("file-explorer-inspector-pane")
            .withChild(io.mindspice.simplypages.components.Header.H4("Inspector"))
            .withChild(new io.mindspice.simplypages.components.Paragraph("Inspecting: " + (path == null ? "" : path)))
            .render();
    }

    @GetMapping("/demos/file-picker/view")
    @ResponseBody
    public String filePickerView(@RequestParam(value = "path", required = false) String path) {
        return new io.mindspice.simplypages.components.Div()
            .withId("demo-file-picker-viewer")
            .withClass("file-explorer-viewer-pane")
            .withChild(io.mindspice.simplypages.components.Header.H4("Viewer"))
            .withChild(new io.mindspice.simplypages.components.Paragraph("Picker preview for: " + (path == null ? "" : path)))
            .render();
    }

    @GetMapping("/demos/file-explorer/modal/{type}")
    @ResponseBody
    public String fileExplorerModal(@org.springframework.web.bind.annotation.PathVariable("type") String type,
                                    @RequestParam(value = "path", required = false) String path) {
        return new io.mindspice.simplypages.components.Div()
            .withId("demo-file-explorer-modal")
            .withAttribute("hx-swap-oob", "true")
            .withClass("file-explorer-modal-container")
            .withChild(io.mindspice.simplypages.components.display.Modal.create()
                .withModalId("demo_file_modal")
                .withTitle("Demo " + type)
                .withBody(new io.mindspice.simplypages.components.Paragraph("Path: " + (path == null ? "" : path))))
            .render();
    }

    @GetMapping("/demos/file-picker/modal/{type}")
    @ResponseBody
    public String filePickerModal(@org.springframework.web.bind.annotation.PathVariable("type") String type,
                                  @RequestParam(value = "path", required = false) String path) {
        return new io.mindspice.simplypages.components.Div()
            .withId("demo-file-picker-modal")
            .withClass("file-explorer-modal-container")
            .withChild(io.mindspice.simplypages.components.display.Modal.create()
                .withModalId("demo_picker_modal")
                .withTitle("Picker " + type)
                .withBody(new io.mindspice.simplypages.components.Paragraph("Path: " + (path == null ? "" : path))))
            .render();
    }

    @GetMapping("/demos/file-explorer/action/run")
    @ResponseBody
    public String fileExplorerAction(@RequestParam(value = "path", required = false) String path) {
        return new io.mindspice.simplypages.components.Paragraph("Ran action for: " + (path == null ? "" : path)).render();
    }

    @GetMapping("/demos/file-picker/action/run")
    @ResponseBody
    public String filePickerAction(@RequestParam(value = "path", required = false) String path) {
        return new io.mindspice.simplypages.components.Paragraph("Ran picker action for: " + (path == null ? "" : path)).render();
    }

    @PostMapping("/demos/file-picker/select")
    @ResponseBody
    public String filePickerSelect(@RequestParam("path") String path) {
        return new io.mindspice.simplypages.components.Div()
            .withId("demo-file-picker-value")
            .withClass("file-picker-value")
            .withInnerText(path)
            .render();
    }

    @GetMapping(value = {"/docs/**", "/docs"})
    @ResponseBody
    public String docs(
        HttpServletRequest request,
        HttpServletResponse response,
        @RequestHeader(value = "HX-Request", required = false) String hxRequest
    ) {
        String path = documentationService.normalizePath(request.getRequestURI());
        String markdown = documentationService.getDocContent(path);

        if (markdown == null) {
            response.setStatus(404);
            return "Documentation not found: " + path;
        }

        String title = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        title = title.replace("-", " ").replace(".md", "");
        title = title.substring(0, 1).toUpperCase() + title.substring(1);

        DocsPage docsPage = new DocsPage(title, markdown, documentationService.getDocsNavigation());
        response.setHeader("Vary", "HX-Request");

        if (hxRequest != null) {
            return docsPage.renderContent();
        }

        return renderInHomeShell(docsPage, null, response);
    }

    @GetMapping("/javadocs-view")
    @ResponseBody
    public String javadocs(
        @RequestHeader(value = "HX-Request", required = false) String hxRequest,
        HttpServletResponse response
    ) {
        return renderInHomeShell(javadocsPage, hxRequest, response);
    }

    @PostMapping("/demos/api/template-card")
    @ResponseBody
    public String templateCard(
        @RequestParam("title") String title,
        @RequestParam("body") String body
    ) {
        return HtmxEditingDemoPage.renderTemplateCard(title, body);
    }

    @PostMapping("/demos/api/form-preview")
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
        response.setHeader("Vary", "HX-Request");
        if (hxRequest != null) {
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
            .build();
    }

    private String renderInDemoShell(DemoPage page, String hxRequest, HttpServletResponse response) {
        response.setHeader("Vary", "HX-Request");
        if (hxRequest != null) {
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
                .addLink("File Explorer", "/demos/file-explorer")
                .addLink("File Picker", "/demos/file-picker")
                .addLink("Chat", "/chat")
                .build())
            .withContent(new RawHtml(page.render()))
            .build();
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
