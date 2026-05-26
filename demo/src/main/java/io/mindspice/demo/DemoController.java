package io.mindspice.demo;

import io.mindspice.demo.pages.*;
import io.mindspice.simplypages.builders.BannerBuilder;
import io.mindspice.simplypages.builders.ShellBuilder;
import io.mindspice.simplypages.builders.SideNavBuilder;
import io.mindspice.simplypages.builders.TopNavBuilder;
import io.mindspice.simplypages.components.RawHtml;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.forms.Autocomplete;
import io.mindspice.simplypages.core.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Controller
public class DemoController {

    private static final List<AutocompleteTopic> AUTOCOMPLETE_TOPICS = List.of(
        new AutocompleteTopic("components", "Components", "Low-level reusable UI primitives", true),
        new AutocompleteTopic("forms", "Forms", "Inputs, choices, validation, and actions", true),
        new AutocompleteTopic("htmx", "HTMX", "Server-rendered fragments and swaps", true),
        new AutocompleteTopic("modules", "Modules", "Composed feature blocks with lifecycle hooks", true),
        new AutocompleteTopic("slotkeys", "SlotKeys", "Request-time dynamic rendering values", true),
        new AutocompleteTopic("legacy-js", "Legacy JavaScript", "Shown as unavailable for contrast", false)
    );

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

    @GetMapping("/demos/api/autocomplete/topics")
    @ResponseBody
    public String autocompleteTopics(
        @RequestParam(value = "topic", required = false) String query,
        @RequestParam(value = "scope", required = false) String scope
    ) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedScope = scope == null || scope.isBlank() ? "framework" : scope;

        List<Autocomplete.OptionRow> rows = new ArrayList<>();
        for (AutocompleteTopic topic : AUTOCOMPLETE_TOPICS) {
            if (!normalizedQuery.isBlank()
                && !topic.value().contains(normalizedQuery)
                && !topic.label().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
                continue;
            }

            String selectUrl = Autocomplete.appendQuery(
                "/demos/api/autocomplete/select-topic",
                Map.of(
                    "topic", topic.value(),
                    "scope", normalizedScope
                )
            );

            rows.add(new Autocomplete.OptionRow(
                topic.value(),
                topic.label(),
                topic.detail(),
                topic.available() ? "available" : "unavailable",
                topic.available(),
                selectUrl
            ));
        }

        return Autocomplete.options(
            new Autocomplete.OptionsConfig(
                "#sp-autocomplete-topic",
                "outerHTML",
                "No framework topics matched"
            ),
            rows
        ).render();
    }

    @GetMapping("/demos/api/autocomplete/select-topic")
    @ResponseBody
    public String selectAutocompleteTopic(
        @RequestParam("topic") String value,
        @RequestParam(value = "scope", required = false) String scope
    ) {
        AutocompleteTopic selected = findAutocompleteTopic(value);
        if (selected == null || !selected.available()) {
            return BasicsFormsDemoPage.demoAutocomplete("", "")
                .withStatus(new Autocomplete.StatusMessage(
                    "Select an available " + (scope == null || scope.isBlank() ? "framework" : scope) + " topic",
                    Autocomplete.State.INVALID
                ))
                .render();
        }

        return BasicsFormsDemoPage.demoAutocomplete(selected.value(), selected.label()).render();
    }

    @GetMapping("/demos/api/autocomplete/topic-status")
    @ResponseBody
    public String autocompleteTopicStatus(@RequestParam(value = "topic", required = false) String value) {
        AutocompleteTopic selected = findAutocompleteTopic(value);
        Autocomplete.State state = selected == null
            ? Autocomplete.State.DEFAULT
            : selected.available()
                ? Autocomplete.State.SELECTED
                : Autocomplete.State.INVALID;
        String text = selected == null
            ? "Search framework topics"
            : selected.available() ? "Selected: " + selected.label() : "Unavailable: " + selected.label();

        return Autocomplete.status(
            new Autocomplete.StatusMessage(text, state)
        ).render();
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

    private static AutocompleteTopic findAutocompleteTopic(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (AutocompleteTopic topic : AUTOCOMPLETE_TOPICS) {
            if (topic.value().equals(value)) {
                return topic;
            }
        }
        return null;
    }

    private record AutocompleteTopic(String value, String label, String detail, boolean available) {
    }
}
