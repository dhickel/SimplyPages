package io.mindspice.demo.pages;

import io.mindspice.demo.SiteShell;
import io.mindspice.simplypages.components.*;
import io.mindspice.simplypages.components.display.Alert;
import io.mindspice.simplypages.components.forms.*;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.*;
import io.mindspice.simplypages.editing.*;
import io.mindspice.simplypages.layout.*;
import io.mindspice.simplypages.modules.ContentModule;
import java.util.List;
import java.util.Map;

public final class FeatureExamples {
    public static final SlotKey<String> TITLE = SlotKey.of("example.title");
    public static final SlotKey<String> BODY = SlotKey.of("example.body");
    public static final Template CARD = new Div().withId("template-result")
        .withChild(new HtmlTag("h3").withChild(Slot.of(TITLE)))
        .withChild(new Paragraph().withChild(Slot.of(BODY))).compile();
    public static final List<String> SLUGS = List.of("templates", "htmx", "editing", "static-content", "forum", "chat", "safe-rendering");
    private FeatureExamples() {}

    public static String directory() {
        Div root = new Div().withClass("sp-site-directory-group").withChild(Header.H2("Framework features"));
        for (String slug : SLUGS) root.withChild(Link.create("/demos/features/" + slug, title(slug)));
        return root.render();
    }
    public static String title(String slug) {
        return switch (slug) {
            case "templates" -> "Typed slots & compiled templates";
            case "htmx" -> "HTMX fragments & forms";
            case "editing" -> "Editing, permissions & proposals";
            case "static-content" -> "Markdown content pipeline";
            case "forum" -> "Forum renderers & actions";
            case "chat" -> "Chat & finite SSE";
            case "safe-rendering" -> "Safe rendering & application boundaries";
            default -> slug;
        };
    }
    public static String render(String slug) {
        Div root = new Div().withChild(new RawHtml(SiteShell.intro(title(slug), "A coherent working example of the framework's rendering and integration contracts.")));
        switch (slug) {
            case "templates", "htmx" -> {
                Form form = Form.create().withAction("/demos/api/template-card").withMethod(Form.Method.POST)
                    .withHxPost("/demos/api/template-card").withHxTarget("#template-result").withHxSwap("outerHTML")
                    .addField("Title", TextInput.create("title").withValue("Request-time title").required())
                    .addField("Body", TextInput.create("body").withValue("A different context, the same compiled structure.").required())
                    .withChild(Button.submit("Render with context"));
                root.withChild(example("typed-slots", "SlotKey · Slot · RenderContext · Template", "Submit to replace the actual rendered values. The template is shared; request data is not.",
                    new Div().withChild(form).withChild(new RawHtml(card("Initial title", "Supply your own request values."))),
                    """
                    public static String typedPreview() {
                        SlotKey<String> title = SlotKey.of("example.title");
                        SlotKey<String> body = SlotKey.of("example.body");
                        Template card = new Div().withId("template-result")
                            .withChild(new HtmlTag("h3").withChild(Slot.of(title)))
                            .withChild(new Paragraph().withChild(Slot.of(body))).compile();
                        Form form = Form.create().withAction("/demos/api/template-card").withMethod(Form.Method.POST)
                            .withHxPost("/demos/api/template-card").withHxTarget("#template-result").withHxSwap("outerHTML")
                            .addField("Title", TextInput.create("title").withValue("Request-time title").required())
                            .addField("Body", TextInput.create("body").withValue("A different context, the same compiled structure.").required())
                            .withChild(Button.submit("Render with context"));
                        return new Div().withChild(form).withChild(new RawHtml(card.render(RenderContext.builder()
                            .with(title, "Initial title").with(body, "Supply your own request values.").build()))).render();
                    }
                    // POST /demos/api/template-card renders that same shared template with submitted title/body.
                    // The application adds CSRF fields and headers to forms before HTTP output.
                    """,
                    "core/Template", "/docs/core/03-template-rendercontext-slotkey-reference.md"));
                root.withChild(example("module-entries", "Module structure & slot entries",
                    "A module builds its structure once. Live entries are escaped at render time; compiled entries accept only application-trusted rendered HTML. Both contexts reuse the same compiled module.",
                    new RawHtml(moduleEntries()),
                    """
                    public static String moduleEntries() {
                        SlotKey<String> value = SlotKey.of("module.value");
                        io.mindspice.simplypages.core.Module module = new io.mindspice.simplypages.core.Module("section") {
                            protected void buildContent() {
                                withChild(new Paragraph().withChild(Slot.of(value)));
                            }
                        };
                        Template template = module.compile();
                        RenderContext live = RenderContext.builder().with(value, "Live <text>").build();
                        RenderContext compiled = RenderContext.builder().build()
                            .putCompiled(value, new HtmlTag("strong").withInnerText("Trusted compiled text").render());
                        SlotEntry liveEntry = live.getEntries().get(value);
                        SlotEntry compiledEntry = compiled.getEntries().get(value);
                        return new Div().withChild(new Paragraph(liveEntry.getClass().getSimpleName()))
                            .withChild(new RawHtml(template.render(live)))
                            .withChild(new Paragraph(compiledEntry.getClass().getSimpleName()))
                            .withChild(new RawHtml(template.render(compiled))).render();
                    }
                    """, "core/SlotEntry", "/docs/core/03-template-rendercontext-slotkey-reference.md"));
                SlotKey<String> bound = SlotKey.of("bound");
                Template view = new Paragraph().withChild(Slot.of(bound)).compile();
                root.withChild(example("bound-context", "TemplateComponent: a bound context",
                    "This adapter intentionally ignores its parent's context. The bound value below wins over the parent's different value.",
                    new RawHtml(TemplateComponent.of(view, RenderContext.builder().with(bound, "Bound value").build())
                        .render(RenderContext.builder().with(bound, "Parent value").build())),
                    "SlotKey<String> bound = SlotKey.of(\"bound\");\nTemplate view = new Paragraph().withChild(Slot.of(bound)).compile();\nTemplateComponent.of(view, RenderContext.builder().with(bound, \"Bound value\").build())\n    .render(RenderContext.builder().with(bound, \"Parent value\").build());",
                    "core/TemplateComponent", "/docs/core/03-template-rendercontext-slotkey-reference.md"));
                root.withChild(example("live-delegates", "Compilation & live delegates", "Custom render(RenderContext) overrides remain live delegates. A compiled view still dispatches the supplied context; sharing requires the delegate itself to be safe.",
                    new RawHtml(liveDelegate().render(RenderContext.builder().with(TITLE, "This value arrived through a live delegate").build())),
                    "SlotKey<String> title = SlotKey.of(\"example.title\");\nComponent live = context -> new Paragraph(context.get(title).orElse(\"\")).render(context);\nTemplate compiled = live.compile();\ncompiled.render(RenderContext.builder().with(title, \"This value arrived through a live delegate\").build());",
                    "core/Component", "/docs/core/04-rendering-pipeline-high-and-low-level.md"));
                root.withChild(new Paragraph("Forms use native POST actions and HTMX outerHTML swaps. The application supplies one hidden CSRF token and literal inherited headers. The editing example demonstrates multi-target OOB page/modal updates, including protected DELETE, validation drafts and permission failures."))
                    .withChild(Link.create("/demos/features/editing", "Try editing and OOB updates →"))
                    .withChild(Button.create("Load editing demo fragment")
                        .withAttribute("hx-get", "/demos/features/editing")
                        .withAttribute("hx-target", "#editing-fragment").withAttribute("hx-swap", "innerHTML"))
                    .withChild(new Div().withId("editing-fragment"));
            }
            case "safe-rendering" -> {
                Form form = Form.create().withAction("/demos/api/safe").withMethod(Form.Method.POST)
                    .withHxPost("/demos/api/safe").withHxTarget("#safe-result").withHxSwap("outerHTML")
                    .addField("Untrusted text", TextInput.create("value").withValue("<script>alert('not executed')</script>").required())
                    .withChild(Button.submit("Render safely"));
                root.withChild(example("escaping", "Text escaping & safe Markdown", "Try HTML-sensitive input. Paragraph escapes text; Markdown escapes raw HTML and sanitizes unsafe links. RawHtml is a deliberately trusted boundary, not a sanitizer.",
                    new Div().withChild(form).withChild(new RawHtml(safe("<strong>Plain text</strong>"))),
                    """
                    public static String safe(String value) {
                        return new Div().withId("safe-result").withAttribute("aria-live", "polite")
                            .withChild(Header.H3("Escaped text")).withChild(new Paragraph(value))
                            .withChild(Header.H3("Safe Markdown")).withChild(new Markdown(value)).render();
                    }
                    public static String safetyPreview() {
                        Form form = Form.create().withAction("/demos/api/safe").withMethod(Form.Method.POST)
                            .withHxPost("/demos/api/safe").withHxTarget("#safe-result").withHxSwap("outerHTML")
                            .addField("Untrusted text", TextInput.create("value").withValue("<script>alert('not executed')</script>").required())
                            .withChild(Button.submit("Render safely"));
                        return new Div().withChild(form).withChild(new RawHtml(safe("<strong>Plain text</strong>"))).render();
                    }
                    // POST /demos/api/safe returns safe(value). RawHtml above wraps framework-rendered HTML only.
                    """, "components/Paragraph", "/docs/security/01-security-boundaries-and-safe-rendering.md"));
                root.withChild(example("url-style-safety", "URL & style validation", "These are actual rejected configuration attempts. Validation is not endpoint authorization, destination policy or CSRF enforcement.",
                    new Div().withChild(new Paragraph(rejectedUrl())).withChild(new Paragraph(rejectedStyle())),
                    """
                    public static String rejectedUrl() {
                        try { SafeUrl.validateHref("javascript:alert(1)"); return "URL accepted"; }
                        catch (IllegalArgumentException rejected) { return "Rejected javascript: link: " + rejected.getMessage(); }
                    }
                    public static String rejectedStyle() {
                        try { new Div().addStyle("color", "red;display:none"); return "Style accepted"; }
                        catch (IllegalArgumentException rejected) { return "Rejected injected style: " + rejected.getMessage(); }
                    }
                    public static String rejectedPreview() {
                        return new Div().withChild(new Paragraph(rejectedUrl())).withChild(new Paragraph(rejectedStyle())).render();
                    }
                    """,
                    "core/SafeUrl", "/docs/security/01-security-boundaries-and-safe-rendering.md"));
                Style style = Style.create().padding("1rem").color("#245C48");
                HtmlTag styled = new Div().withInnerText("Application-owned style values");
                styled.withAttribute("style", style.getStyleString());
                CssClassNames.addTokens(styled, "example-style", "example-style");
                root.withChild(example("style-composition", "Style & class composition", "Style is a raw application-owned fragment builder, not a CSS sanitizer. CssClassNames combines class tokens.", styled,
                    "Style style = Style.create().padding(\"1rem\").color(\"#245C48\");\nHtmlTag styled = new Div().withInnerText(\"Application-owned style values\");\nstyled.withAttribute(\"style\", style.getStyleString());\nCssClassNames.addTokens(styled, \"example-style\", \"example-style\");\nstyled.render();",
                    "core/Style", "/docs/core/05-css-defaults-overrides-and-structure.md"));
                root.withChild(authExample(false));
            }
            case "static-content" -> root
                .withChild(example("content-pipeline", "StaticContentSiteBuilder · sections · route index · diagnostics", "The local Markdown publication is generated from packaged files. Its index, pagination and details are actual framework components, not a client-side content mock.",
                    new Div().withChild(Link.create("/blog", "Open the generated publication →")),
                    "// The preview link opens the actual generated publication:\nnew Div().withChild(Link.create(\"/blog\", \"Open the generated publication →\"));\n// Application setup: directory is the extracted packaged Markdown directory.\nContentSiteBundle bundle = StaticContentSiteBuilder.create()\n    .addSection(ContentSectionConfig.create(\"blog\", directory, \"/blog\")\n        .withSectionTitle(\"SimplyPages Blog\").withPageSize(5))\n    .build().generate();\nbundle.routeIndex().resolveRequest(path, page);\nbundle.warnings(); // inspect section, source and message",
                    "components/content/StaticContentSiteBuilder", "/docs/reference/content-helper-api-reference.md"))
                .withChild(new Paragraph("ContentEntryRecord and section/site bundles carry generated metadata. DefaultContentListItemComponent composes the index; ContentRouteIndex resolves details before indexes. ContentWarning reports generation diagnostics. Applications own file admission, publication rules and persistence."));
            default -> { return null; }
        }
        return root.render();
    }
    public static String card(String title, String body) {
        return CARD.render(RenderContext.builder().with(TITLE, title).with(BODY, body).build());
    }
    private static String moduleEntries() {
        SlotKey<String> value = SlotKey.of("module.value");
        io.mindspice.simplypages.core.Module module = new io.mindspice.simplypages.core.Module("section") {
            protected void buildContent() {
                withChild(new Paragraph().withChild(Slot.of(value)));
            }
        };
        Template template = module.compile();
        RenderContext live = RenderContext.builder().with(value, "Live <text>").build();
        RenderContext compiled = RenderContext.builder().build()
            .putCompiled(value, new HtmlTag("strong").withInnerText("Trusted compiled text").render());
        SlotEntry liveEntry = live.getEntries().get(value);
        SlotEntry compiledEntry = compiled.getEntries().get(value);
        return new Div().withChild(new Paragraph(liveEntry.getClass().getSimpleName()))
            .withChild(new RawHtml(template.render(live)))
            .withChild(new Paragraph(compiledEntry.getClass().getSimpleName()))
            .withChild(new RawHtml(template.render(compiled))).render();
    }
    private static Template liveDelegate() {
        Component live = context -> new Paragraph(context.get(TITLE).orElse("")).render(context);
        return live.compile();
    }
    public static String safe(String value) {
        return new Div().withId("safe-result").withAttribute("aria-live", "polite")
            .withChild(Header.H3("Escaped text")).withChild(new Paragraph(value))
            .withChild(Header.H3("Safe Markdown")).withChild(new Markdown(value)).render();
    }
    private static String rejectedUrl() {
        try { SafeUrl.validateHref("javascript:alert(1)"); return "URL accepted"; }
        catch (IllegalArgumentException rejected) { return "Rejected javascript: link: " + rejected.getMessage(); }
    }
    private static String rejectedStyle() {
        try { new Div().addStyle("color", "red;display:none"); return "Style accepted"; }
        catch (IllegalArgumentException rejected) { return "Rejected injected style: " + rejected.getMessage(); }
    }
    public static String editingExtras() {
        EditableRow row = EditableRow.wrap(new Row().withChild(new Column().withWidth(12)
            .withChild(ContentModule.create().withTitle("Read-only wrapper specimen").withContent("This row intentionally has no edit controls. Use the live sandbox above to mutate content."))), "wrapper-row", "wrapper-page").withCanAddModule(false);
        EditablePage page = EditablePage.create("wrapper-page").withInsertRowUrlTemplate("/demos/features/editing/insert-wrapper-row").addEditableRow(row);
        return example("editablepage", "EditablePage & EditableRow", "A real wrapper composition. Its insertion control uses a presentation adapter for the existing first-row insertion action, returning the live sandbox page and its canonical modal host through OOB output. The wrapper row is a read-only variant, not a second datastore.", page,
            "Row rowContent = new Row().withChild(new Column().withWidth(12)\n    .withChild(ContentModule.create().withTitle(\"Read-only wrapper specimen\")\n        .withContent(\"This row intentionally has no edit controls. Use the live sandbox above to mutate content.\")));\nEditableRow row = EditableRow.wrap(rowContent, \"wrapper-row\", \"wrapper-page\")\n    .withCanAddModule(false);\nEditablePage.create(\"wrapper-page\")\n    .withInsertRowUrlTemplate(\"/demos/features/editing/insert-wrapper-row\")\n    .addEditableRow(row);", "editing/EditablePage", "/docs/reference/editing-api-reference.md").render()
            + liveEditingContracts() + editFields("A slot-driven draft", false, null)
            + example("module-edit-handler", "ModuleEditHandler: request-owned preview", "Each submission creates a new ContentModule. Update renders a copy; delete omits that copy. USER mode renders a proposal preview only. Nothing is saved, queued or deleted in the sandbox above.",
                new RawHtml(handlerResult("edit", "Request-owned title", "Edit this temporary copy.", EditMode.OWNER_EDIT)),
                HANDLER_SOURCE, "editing/ModuleEditHandler", "/docs/reference/editing-api-reference.md").render()
            + authExample(false).render();
    }
    private static String liveEditingContracts() {
        return example("live-editing-contracts", "Editable · EditableChild · EditModalBuilder · EditMode",
            "The live sandbox above reconstructs Editable modules from this session. Open Nested Editing Demo to edit its positional children; choose USER mode to stage a content proposal, then use Pending Edits above to approve or reject it. The listing is the module-9 modal composition with its resolved URLs. The controller supplies editable from the current session's SimpleListModule and combinedForm from its property fields, sandbox notice and width selector; neither is a second datastore. Reset the sandbox if you deleted that module.",
            new Div().withChild(Button.create("Open live nested edit")
                .withAttribute("hx-get", "/editing-demo/edit/module-9?editMode=OWNER_EDIT")
                .withAttribute("hx-target", "#edit-modal-container"))
                .withChild(Button.create("Open live nested proposal")
                    .withAttribute("hx-get", "/editing-demo/edit/module-9?editMode=USER_EDIT")
                    .withAttribute("hx-target", "#edit-modal-container")),
            """
            public static Component nestedTriggers() {
                return new Div().withChild(Button.create("Open live nested edit")
                    .withAttribute("hx-get", "/editing-demo/edit/module-9?editMode=OWNER_EDIT")
                    .withAttribute("hx-target", "#edit-modal-container"))
                    .withChild(Button.create("Open live nested proposal")
                        .withAttribute("hx-get", "/editing-demo/edit/module-9?editMode=USER_EDIT")
                        .withAttribute("hx-target", "#edit-modal-container"));
            }
            // Corresponding module-9 composition: arguments come from the live controller.
            // EditModalBuilder consumes editable.getEditableChildren(): List<EditableChild>.
            public static String nestedModal(Editable<?> editable, Component combinedForm, EditMode mode) {
                return EditModalBuilder.create().withTitle("Edit Module").withModuleId("module-9")
                    .withEditView(combinedForm).withEditable(editable)
                    .withSaveUrl("/editing-demo/save/module-9?editMode=" + mode.name())
                    .withDeleteUrl("/editing-demo/delete/module-9?editMode=" + mode.name())
                    .withChildEditUrl("/editing-demo/edit-child/module-9/{id}?editMode=" + mode.name())
                    .withChildDeleteUrl("/editing-demo/delete-child/module-9/{id}?editMode=" + mode.name())
                    .withPageContainerId("page-content").withModalContainerId("edit-modal-container").build().render();
            }
            // OWNER_EDIT applies to live session content; USER_EDIT stages a proposal.
            """,
            "editing/EditModalBuilder", "/docs/reference/editing-api-reference.md").render();
    }
    private static final String FIELDS_SOURCE = """
        public static String editFieldsResult(String title, boolean markdown, String error) {
            SlotKey<String> key = SlotKey.of("draftTitle");
            SlotKey<Boolean> enabled = SlotKey.of("markdown");
            Component fields = EditFormBuilder.fromSlots(java.util.Map.of("draftTitle", key, "markdown", enabled),
                java.util.Map.of("draftTitle", String.class, "markdown", Boolean.class),
                SlotKeyMap.create().putString("draftTitle", title).putBoolean("markdown", markdown));
            Form form = Form.create().withAction("/demos/api/edit-fields").withMethod(Form.Method.POST)
                .withHxPost("/demos/api/edit-fields").withHxTarget("#edit-fields-result").withHxSwap("outerHTML")
                .withChild(fields).withChild(Button.submit("Validate draft preview"));
            Div preview = new Div().withId("edit-fields-result").withAttribute("aria-live", "polite");
            if (error != null) preview.withChild(Alert.danger(error).withAttribute("role", "alert"));
            ContentModule result = ContentModule.create().withTitle(error == null ? "Validated preview" : "Draft · not accepted").withContent(title);
            if (!markdown) result.disableMarkdown();
            return preview.withChild(form).withChild(result).render();
        }
        // POST /demos/api/edit-fields validates before returning this same draft:
        // ValidationResult validation = draftTitle.isBlank() || draftTitle.length() > 80
        //     ? ValidationResult.invalid("Use a nonblank title of at most 80 characters.")
        //     : ValidationResult.valid();
        // return editFieldsResult(draftTitle, markdown, validation.isValid() ? null : validation.getErrorsAsString());
        // EditFormBuilder delegates String/Boolean controls to FormFieldHelper.
        // SlotKeyMap stores TypedValue entries; it is not shared between requests.
        """;
    public static String editFieldsResult(String title, boolean markdown, String error) {
        SlotKey<String> key = SlotKey.of("draftTitle");
        SlotKey<Boolean> enabled = SlotKey.of("markdown");
        Component fields = EditFormBuilder.fromSlots(java.util.Map.of("draftTitle", key, "markdown", enabled),
            java.util.Map.of("draftTitle", String.class, "markdown", Boolean.class),
            SlotKeyMap.create().putString("draftTitle", title).putBoolean("markdown", markdown));
        Form form = Form.create().withAction("/demos/api/edit-fields").withMethod(Form.Method.POST)
            .withHxPost("/demos/api/edit-fields").withHxTarget("#edit-fields-result").withHxSwap("outerHTML")
            .withChild(fields).withChild(Button.submit("Validate draft preview"));
        Div preview = new Div().withId("edit-fields-result").withAttribute("aria-live", "polite");
        if (error != null) preview.withChild(Alert.danger(error).withAttribute("role", "alert"));
        ContentModule result = ContentModule.create().withTitle(error == null ? "Validated preview" : "Draft · not accepted").withContent(title);
        if (!markdown) result.disableMarkdown();
        return preview.withChild(form).withChild(result).render();
    }
    public static String editFields(String title, boolean markdown, String error) {
        Component preview = new RawHtml(editFieldsResult(title, markdown, error));
        return example("edit-form-builder", "Slot-driven edit fields & validation", "EditFormBuilder derives controls using FormFieldHelper and SlotKeyMap. Submit a nonblank title of at most 80 characters. This is a stateless preview, separate from the mutable sandbox above; invalid input retains its draft.", preview,
            FIELDS_SOURCE,
            "editing/EditFormBuilder", "/docs/reference/editing-api-reference.md").render();
    }
    public static Component authExample(boolean allow) {
        return example("authwrapper", "AuthWrapper: simulated policy", "Toggle the simulated check. This demonstrates action gating, not login or real authorization.",
            new Div().withChild(Form.create().withAction("/demos/api/auth").withMethod(Form.Method.POST)
                .withHxPost("/demos/api/auth").withHxTarget("#auth-result").withHxSwap("outerHTML")
                .withChild(Checkbox.create("allow", "true").withLabel("Simulate permission granted"))
                .withChild(Button.submit("Evaluate policy"))).withChild(new RawHtml(auth(allow))),
            POLICY_SOURCE,
            "editing/AuthWrapper", "/docs/security/02-authwrapper-authorizationchecker-integration.md");
    }
    private static final String POLICY_SOURCE = """
        public static AuthorizationChecker policy(boolean allow) {
            return new AuthorizationChecker() {
                public boolean canEdit(String moduleId, String userId) { return allow && "specimen".equals(moduleId) && "reviewer".equals(userId); }
                public boolean canDelete(String moduleId, String userId) { return allow && "specimen".equals(moduleId) && "owner".equals(userId); }
                public EditMode getEditMode(String moduleId, String userId) {
                    return canDelete(moduleId, userId) ? EditMode.OWNER_EDIT : EditMode.USER_EDIT;
                }
            };
        }
        public static String auth(boolean allow) {
            AuthorizationChecker checker = policy(allow);
            return new Div().withId("auth-result").withAttribute("role", "status")
                .withChild(new RawHtml(AuthWrapper.require(() -> checker.canEdit("specimen", "reviewer"),
                    () -> Alert.success("Simulated action allowed").render(), () -> Alert.warning("Simulated action denied").render())))
                .withChild(new Paragraph("Reviewer mode: " + checker.getEditMode("specimen", "reviewer")))
                .withChild(new Paragraph("Reviewer may delete: " + checker.canDelete("specimen", "reviewer")))
                .withChild(new Paragraph("Owner may delete: " + checker.canDelete("specimen", "owner"))).render();
        }
        public static String authPreview() {
            return new Div().withChild(Form.create().withAction("/demos/api/auth").withMethod(Form.Method.POST)
                .withHxPost("/demos/api/auth").withHxTarget("#auth-result").withHxSwap("outerHTML")
                .withChild(Checkbox.create("allow", "true").withLabel("Simulate permission granted"))
                .withChild(Button.submit("Evaluate policy"))).withChild(new RawHtml(auth(false))).render();
        }
        // The checkbox POSTs allow to /demos/api/auth; the response replaces #auth-result.
        """;
    public static AuthorizationChecker policy(boolean allow) {
        return new AuthorizationChecker() {
            public boolean canEdit(String moduleId, String userId) { return allow && "specimen".equals(moduleId) && "reviewer".equals(userId); }
            public boolean canDelete(String moduleId, String userId) { return allow && "specimen".equals(moduleId) && "owner".equals(userId); }
            public EditMode getEditMode(String moduleId, String userId) {
                return canDelete(moduleId, userId) ? EditMode.OWNER_EDIT : EditMode.USER_EDIT;
            }
        };
    }
    public static String auth(boolean allow) {
        AuthorizationChecker checker = policy(allow);
        return new Div().withId("auth-result").withAttribute("role", "status")
            .withChild(new RawHtml(AuthWrapper.require(() -> checker.canEdit("specimen", "reviewer"),
                () -> Alert.success("Simulated action allowed").render(), () -> Alert.warning("Simulated action denied").render())))
            .withChild(new Paragraph("Reviewer mode: " + checker.getEditMode("specimen", "reviewer")))
            .withChild(new Paragraph("Reviewer may delete: " + checker.canDelete("specimen", "reviewer")))
            .withChild(new Paragraph("Owner may delete: " + checker.canDelete("specimen", "owner"))).render();
    }

    private static final String HANDLER_SOURCE = """
        public static final class PreviewHandler implements ModuleEditHandler<ContentModule> {
            private final String title;
            private final String content;
            private final EditMode mode;
            public PreviewHandler(String title, String content, EditMode mode) {
                this.title = title;
                this.content = content;
                this.mode = mode;
            }
            private ContentModule copy(String id) {
                return ContentModule.create().withModuleId(id).withTitle(title).withContent(content).disableMarkdown();
            }
            public Component renderEditForm(String id) {
                return Form.create().withAction("/demos/api/module-preview").withMethod(Form.Method.POST)
                    .withHxPost("/demos/api/module-preview").withHxTarget("#handler-result").withHxSwap("outerHTML")
                    .withChild(FormFieldHelper.textField("Preview title", "title", title))
                    .withChild(FormFieldHelper.textAreaField("Preview content", "content", content, 3))
                    .addField("Operation", Select.create("operation").addOption("edit", "Render edit preview")
                        .addOption("update", "Update copy", true).addOption("delete", "Delete copy"))
                    .addField("Preview mode", Select.create("mode").addOption("OWNER_EDIT", "OWNER_EDIT", mode == EditMode.OWNER_EDIT)
                        .addOption("USER_EDIT", "USER_EDIT", mode == EditMode.USER_EDIT))
                    .withChild(Button.submit("Apply preview request"));
            }
            public Component handleUpdate(String id, Map<String, String> data, EditMode editMode) {
                ContentModule candidate = copy(id);
                ValidationResult validation = candidate.validate(data);
                if (!validation.isValid()) return Alert.danger(validation.getErrorsAsString()).withAttribute("role", "alert");
                candidate.applyEdits(data);
                return new Div().withChild(new Paragraph(editMode == EditMode.USER_EDIT
                    ? "Proposal preview only — not queued." : "Updated request-owned copy — not saved."))
                    .withChild(candidate);
            }
            public Component handleDelete(String id, EditMode editMode) {
                return editMode == EditMode.USER_EDIT
                    ? new Div().withChild(new Paragraph("Delete proposal preview only — not queued.")).withChild(copy(id))
                    : new Paragraph("Deleted request-owned preview copy — no stored content changed.");
            }
        }
        public static String handlerResult(String operation, String title, String content, EditMode mode) {
            ModuleEditHandler<ContentModule> handler = new PreviewHandler(title, content, mode);
            Component result = switch (operation) {
                case "edit" -> ContentModule.create().withTitle(title).withContent(content).disableMarkdown();
                case "update" -> handler.handleUpdate("request-preview", Map.of("title", title, "content", content), mode);
                case "delete" -> handler.handleDelete("request-preview", mode);
                default -> throw new IllegalArgumentException("Unknown preview operation");
            };
            return new Div().withId("handler-result").withAttribute("aria-live", "polite")
                .withChild(handler.renderEditForm("request-preview")).withChild(new Div().withId("handler-copy").withChild(result)).render();
        }
        // Protected POST /demos/api/module-preview parses operation/title/content/mode,
        // then calls handlerResult. No handler or module instance is retained between requests.
        """;
    public static final class PreviewHandler implements ModuleEditHandler<ContentModule> {
        private final String title;
        private final String content;
        private final EditMode mode;
        public PreviewHandler(String title, String content, EditMode mode) {
            this.title = title;
            this.content = content;
            this.mode = mode;
        }
        private ContentModule copy(String id) {
            return ContentModule.create().withModuleId(id).withTitle(title).withContent(content).disableMarkdown();
        }
        public Component renderEditForm(String id) {
            return Form.create().withAction("/demos/api/module-preview").withMethod(Form.Method.POST)
                .withHxPost("/demos/api/module-preview").withHxTarget("#handler-result").withHxSwap("outerHTML")
                .withChild(FormFieldHelper.textField("Preview title", "title", title))
                .withChild(FormFieldHelper.textAreaField("Preview content", "content", content, 3))
                .addField("Operation", Select.create("operation").addOption("edit", "Render edit preview")
                    .addOption("update", "Update copy", true).addOption("delete", "Delete copy"))
                .addField("Preview mode", Select.create("mode").addOption("OWNER_EDIT", "OWNER_EDIT", mode == EditMode.OWNER_EDIT)
                    .addOption("USER_EDIT", "USER_EDIT", mode == EditMode.USER_EDIT))
                .withChild(Button.submit("Apply preview request"));
        }
        public Component handleUpdate(String id, Map<String, String> data, EditMode editMode) {
            ContentModule candidate = copy(id);
            ValidationResult validation = candidate.validate(data);
            if (!validation.isValid()) return Alert.danger(validation.getErrorsAsString()).withAttribute("role", "alert");
            candidate.applyEdits(data);
            return new Div().withChild(new Paragraph(editMode == EditMode.USER_EDIT
                ? "Proposal preview only — not queued." : "Updated request-owned copy — not saved."))
                .withChild(candidate);
        }
        public Component handleDelete(String id, EditMode editMode) {
            return editMode == EditMode.USER_EDIT
                ? new Div().withChild(new Paragraph("Delete proposal preview only — not queued.")).withChild(copy(id))
                : new Paragraph("Deleted request-owned preview copy — no stored content changed.");
        }
    }
    public static String handlerResult(String operation, String title, String content, EditMode mode) {
        ModuleEditHandler<ContentModule> handler = new PreviewHandler(title, content, mode);
        Component result = switch (operation) {
            case "edit" -> ContentModule.create().withTitle(title).withContent(content).disableMarkdown();
            case "update" -> handler.handleUpdate("request-preview", Map.of("title", title, "content", content), mode);
            case "delete" -> handler.handleDelete("request-preview", mode);
            default -> throw new IllegalArgumentException("Unknown preview operation");
        };
        return new Div().withId("handler-result").withAttribute("aria-live", "polite")
            .withChild(handler.renderEditForm("request-preview")).withChild(new Div().withId("handler-copy").withChild(result)).render();
    }
    public static String forumReference() {
        return new HtmlTag("details").withClass("sp-site-source-details")
            .withChild(new HtmlTag("summary").withInnerText("Java composition & forum API"))
            .withChild(new Paragraph("The corresponding renderer composition below makes application-supplied values explicit parameters. The live controller supplies its category component, checked action URLs, body preview and thread-link functions, tag registry and current pagination snapshot. These functions are application policy, not framework defaults."))
            .withChild(Code.block("""
                public static <T extends ForumCategoryData, C> Component categories(
                        List<T> values, C context, java.util.function.Supplier<? extends ForumCategoryComponent> component) {
                    return ForumCategoryRenderer.<T, C>builder().withCategoryComponentSupplier(component)
                        .build().render(values, context);
                }
                public static <T extends ForumTopicData, C> Component topics(
                        List<T> values, C viewer, ForumActionProvider<T, C> actions,
                        java.util.function.BiFunction<T, C, String> preview,
                        java.util.function.BiFunction<T, C, ForumTopicTitleLink> links,
                        ForumTagResolverRegistry tags, ForumTopicRenderer.TopicPagination pagination,
                        ForumTopicRenderer.TopicPaginationEndpointResolver endpoint) {
                    return ForumTopicRenderer.<T, C>builder().withActionProvider(actions)
                        .withBodyTextResolver(preview).withTitleLinkResolver(links).withResolverRegistry(tags)
                        .withPaginationHxTarget("#forum-main").withPaginationHxSwap("outerHTML")
                        .withPaginationEndpointResolver(endpoint).build().render(values, viewer, pagination);
                }
                """).withLanguage("java").withTitle("Renderer composition with application inputs"))
            .withChild(example("default-category", "Default category & custom navigation variant",
                "The plain default category is a read-only summary. The live categories below use an application-supplied component to add real navigation links.",
                io.mindspice.simplypages.components.forum.categories.DefaultForumCategoryComponent.create()
                    .withCategoryId("specimen").withTitle("Example category").withDescription("A default summary, not a navigation action.").withTopicCount(3),
                "DefaultForumCategoryComponent.create().withCategoryId(\"specimen\")\n    .withTitle(\"Example category\")\n    .withDescription(\"A default summary, not a navigation action.\").withTopicCount(3);",
                "components/forum/categories/DefaultForumCategoryComponent", "/docs/reference/forum-helper-api-reference.md"))
            .withChild(new Paragraph("The live example uses category, topic and comment renderers, default topic/comment components, ForumCollapsibleComposer, ForumActionProvider and DefaultForumActionProvider. Data interfaces are implemented by application views. ForumTagParser and a resolver registry expand the seeded quote tags; endpoints enforce the same simulated persona permissions shown in the controls."))
            .withChild(Link.create("/docs/reference/forum-helper-api-reference.md", "Forum helper reference"))
            .withChild(Link.create("/javadocs/io/mindspice/simplypages/components/forum/topics/ForumTopicRenderer.html", "ForumTopicRenderer API"))
            .withChild(new RawHtml(FeatureCatalog.references("forum"))).render();
    }
    public static String chatReference() {
        return new HtmlTag("details").withClass("sp-site-source-details")
            .withChild(new HtmlTag("summary").withInnerText("Java composition & chat API"))
            .withChild(Code.block("""
                public static Component chatSpecimen(String conversationId, List<ChatMessageView> history) {
                    ChatTranscriptRenderer<ChatMessageView, Void> renderer =
                        ChatTranscriptRenderer.<ChatMessageView, Void>builder()
                            .withEmptyStateText("No messages yet. Start by sending one below.")
                            .withBodyTextResolver((message, context) -> message.body()).build();
                    Component transcript = new HtmlTag("div").withId("chat-history")
                        .withAttribute("data-chat-conversation-id", conversationId)
                        .withChild(Alert.warning("Temporary, non-published chat. Session ownership is not authentication. Same-origin and CSRF checks are enforced. Only the welcome and latest 25 complete exchanges are retained."))
                        .withChild(renderer.render(history, null));
                    TextInput input = TextInput.create("message").withPlaceholder("Type a message").required();
                    input.withAttribute("maxlength", "4096");
                    input.withAttribute("autocomplete", "off");
                    input.withAttribute("autocorrect", "off");
                    input.withAttribute("autocapitalize", "off");
                    input.withAttribute("spellcheck", "false");
                    Form composer = Form.create().withId("chat-composer").withHxPost("/chat/messages")
                        .withHxTarget("#chat-history").withHxSwap("outerHTML").addField("Message", input);
                    composer.withAttribute("autocomplete", "off");
                    composer.withChild(new HtmlTag("input", true).withAttribute("type", "hidden")
                        .withAttribute("name", "conversationId").withAttribute("value", conversationId));
                    composer.withChild(Button.submit("Send"));
                    return ChatModule.create().withModuleId("chat-module").withTitle("ChatModule")
                        .withDescription("A real transcript and composer with application-owned session history and finite SSE.")
                        .withTranscript(transcript).withComposer(composer)
                        .withUiConfig(new ChatUiConfig(conversationId, ChatTransportMode.SSE,
                            "/chat/history", "/chat/stream", "#chat-history", "outerHTML", null));
                }
                // Equivalent to ChatDemoPage's module plus its complete transcript/composer helpers.
                // The application owns transport coordination, identity, limits and CSRF.
                """).withLanguage("java").withTitle("ChatDemoPage module composition"))
            .withChild(new Paragraph("ChatMessageData supplies message values; DefaultChatMessageComponent renders each message through the transcript renderer. ChatTransportMode and ChatUiConfig describe routing metadata, not an implemented WebSocket server. This site demonstrates SSE only."))
            .withChild(Link.create("/docs/reference/chat-helper-api-reference.md", "Chat helper reference"))
            .withChild(Link.create("/javadocs/io/mindspice/simplypages/modules/ChatModule.html", "ChatModule API")).render();
    }
    public static Component example(String id, String title, String purpose, Component preview, String source, String api, String docs) {
        return new HtmlTag("section").withId(id).withClass("sp-site-example")
            .withChild(Header.H2(title)).withChild(new Paragraph(purpose))
            .withChild(new Div().withClass("sp-site-example-grid")
                .withChild(new Div().withClass("sp-site-preview").withAttribute("data-example-preview", "true").withChild(preview))
                .withChild(Code.block(source).withLanguage("java").withTitle("Java composition")))
            .withChild(new Div().withClass("sp-site-reference-links")
                .withChild(Link.create("/javadocs/io/mindspice/simplypages/" + api + ".html", "Java API"))
                .withChild(Link.create(docs, "Documentation")));
    }
}
