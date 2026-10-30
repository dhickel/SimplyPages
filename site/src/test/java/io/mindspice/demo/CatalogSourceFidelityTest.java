package io.mindspice.demo;

import io.mindspice.demo.pages.*;
import io.mindspice.simplypages.core.Component;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CatalogSourceFidelityTest {
    @TempDir Path output;
    private static final String IMPORTS = """
        import java.util.*;
        import io.mindspice.simplypages.core.*;
        import io.mindspice.simplypages.components.*;
        import io.mindspice.simplypages.components.display.*;
        import io.mindspice.simplypages.components.forms.*;
        import io.mindspice.simplypages.components.media.*;
        import io.mindspice.simplypages.components.navigation.*;
        import io.mindspice.simplypages.layout.*;
        import io.mindspice.simplypages.modules.*;
        import io.mindspice.simplypages.builders.*;
        import io.mindspice.simplypages.editing.*;
        """;

    @Test void displayedCatalogJavaCompilesAndReproducesEveryInlineSpecimen() throws Exception {
        var entries = SiteCatalog.examples().stream().filter(e -> !e.route().startsWith("/demos/features/")
            && !Set.of("ShellBuilder", "ShellTemplate").contains(e.symbol().getSimpleName())).toList();
        StringBuilder java = new StringBuilder(IMPORTS).append("public class Listings {\n");
        for (int i = 0; i < entries.size(); i++) {
            var entry = entries.get(i);
            var section = Jsoup.parse(SiteCatalog.section(entry).render());
            String source = section.selectFirst(".sp-site-example-grid > .code-container code").wholeText();
            // Source is taken from the rendered listing, not a second test-only example.
            if (entry.symbol().getSimpleName().equals("Modal")) source = source.substring(0, source.indexOf("// GET"));
            if (entry.symbol().getSimpleName().startsWith("Dynamic")) {
                source = source.substring(0, source.indexOf("// POST"));
                source = source.replace("\nnew Div()", "\nreturn new Div()");
            } else if (Set.of("Form", "FormModule").contains(entry.symbol().getSimpleName())) {
                source = source.replace("\nform.render();", "\nreturn form.render();")
                    .replace("\nFormModule.create()", "\nreturn FormModule.create()");
            } else source = "return " + source;
            java.append("public static Object example").append(i).append("() {\n").append(source).append("\n}\n");
        }
        java.append("}\n");
        Class<?> listings = compile("Listings", java.toString());
        for (int i = 0; i < entries.size(); i++) {
            Object result = listings.getMethod("example" + i).invoke(null);
            String actual = result instanceof Component c ? c.render() : result.toString();
            assertEquals(normalize(entries.get(i).preview().get().render()), normalize(actual), entries.get(i).destination());
        }
        assertEquals(70, entries.size(), "All inline specimens must be exercised; linked full documents are checked separately");
    }

    @Test void fullDocumentListingsReproduceTheirIsolatedBuilderRoutes() throws Exception {
        for (String kind : List.of("shell", "shell-template")) {
            var example = SiteCatalog.examples().stream().filter(e -> e.route().equals("/demos/builders/" + kind)).findFirst().orElseThrow();
            String source = Jsoup.parse(SiteCatalog.section(example).render()).selectFirst(".sp-site-example-grid > .code-container code").wholeText();
            source = source.replace("\nshell.", "\nreturn shell.");
            String name = kind.equals("shell") ? "FullShell" : "TemplateShell";
            Class<?> listing = compile(name, IMPORTS + "public class " + name + " { public static String render() {\n" + source + "\n}}\n");
            String expected = new DemoController(null, null).shellPreview(kind, new org.springframework.mock.web.MockHttpServletResponse());
            assertEquals(normalize(expected), normalize((String) listing.getMethod("render").invoke(null)), kind);
        }
    }

    @Test void handlerAndPolicyListingsAreActualExecutableHelpers() throws Exception {
        var editing = Jsoup.parse(FeatureExamples.editingExtras());
        String handler = editing.selectFirst("#module-edit-handler .code-block code").wholeText();
        String policy = editing.selectFirst("#authwrapper .code-block code").wholeText();
        String fields = editing.selectFirst("#edit-form-builder .code-block code").wholeText();
        Class<?> helpers = compile("Helpers", IMPORTS + "public class Helpers {\n" + handler + policy + fields + "\n}");
        assertEquals(normalize(editing.selectFirst("#authwrapper [data-example-preview]").html()),
            normalize((String) helpers.getMethod("authPreview").invoke(null)));
        for (boolean markdown : List.of(false, true)) {
            for (String error : new String[]{null, "Invalid <draft>"}) assertEquals(
                normalize(FeatureExamples.editFieldsResult("Draft <&>", markdown, error)),
                normalize((String) helpers.getMethod("editFieldsResult", String.class, boolean.class, String.class)
                    .invoke(null, "Draft <&>", markdown, error)));
        }
        for (String operation : List.of("edit", "update", "delete")) {
            for (var mode : io.mindspice.simplypages.editing.EditMode.values()) {
                String rendered = (String) helpers.getMethod("handlerResult", String.class, String.class, String.class, mode.getClass())
                    .invoke(null, operation, "Title <&>", "Body <&>", mode);
                assertEquals(normalize(FeatureExamples.handlerResult(operation, "Title <&>", "Body <&>", mode)), normalize(rendered));
            }
        }
        for (boolean allow : List.of(false, true)) assertEquals(FeatureExamples.auth(allow), helpers.getMethod("auth", boolean.class).invoke(null, allow));
        var handlerInstance = new FeatureExamples.PreviewHandler("Old title", "Old body", io.mindspice.simplypages.editing.EditMode.OWNER_EDIT);
        String updated = handlerInstance.handleUpdate("copy", Map.of("title", "New title", "content", "New <body>"),
            io.mindspice.simplypages.editing.EditMode.OWNER_EDIT).render();
        assertTrue(Jsoup.parse(updated).text().contains("New <body>"));
        assertFalse(Jsoup.parse(updated).text().contains("Old body"));
        assertTrue(handlerInstance.renderEditForm("copy").render().contains("Old title"), "Update must not mutate the request's source values");
        var checker = FeatureExamples.policy(true);
        assertTrue(checker.canEdit("specimen", "reviewer"));
        assertFalse(checker.canDelete("specimen", "reviewer"));
        assertTrue(checker.canDelete("specimen", "owner"));
        assertFalse(checker.canEdit("other", "reviewer"));
        assertEquals(io.mindspice.simplypages.editing.EditMode.USER_EDIT, checker.getEditMode("specimen", "reviewer"));
        assertEquals(io.mindspice.simplypages.editing.EditMode.OWNER_EDIT, checker.getEditMode("specimen", "owner"));
    }

    @Test void featurePreviewListingsReproduceActualTemplateAndSafetyOutput() throws Exception {
        var templates = Jsoup.parse(FeatureExamples.render("templates"));
        var safety = Jsoup.parse(FeatureExamples.render("safe-rendering"));
        String typed = templates.selectFirst("#typed-slots .code-block code").wholeText();
        String safe = safety.selectFirst("#escaping .code-block code").wholeText();
        String rejected = safety.selectFirst("#url-style-safety .code-block code").wholeText();
        Class<?> helpers = compile("FeatureListings", IMPORTS + "public class FeatureListings {" + typed + safe + rejected + "}");
        for (var item : List.of(Map.entry(templates.getElementById("typed-slots"), "typedPreview"),
            Map.entry(safety.getElementById("escaping"), "safetyPreview"), Map.entry(safety.getElementById("url-style-safety"), "rejectedPreview"))) {
            assertEquals(normalize(item.getKey().selectFirst("[data-example-preview]").html()),
                normalize((String) helpers.getMethod(item.getValue()).invoke(null)), item.getValue());
        }
        String modal = Jsoup.parse(FeatureExamples.editingExtras()).selectFirst("#live-editing-contracts .code-block code").wholeText();
        Class<?> live = compile("LiveListing", IMPORTS + "public class LiveListing {" + modal + "}");
        assertEquals(normalize(Jsoup.parse(FeatureExamples.editingExtras()).selectFirst("#live-editing-contracts [data-example-preview]").html()),
            normalize(((Component) live.getMethod("nestedTriggers").invoke(null)).render()));
        var list = io.mindspice.simplypages.modules.SimpleListModule.create().withModuleId("module-9")
            .addItem(io.mindspice.simplypages.components.ListItem.create("Actual child"));
        for (var mode : io.mindspice.simplypages.editing.EditMode.values()) {
            var doc = Jsoup.parse((String) live.getMethod("nestedModal", io.mindspice.simplypages.editing.Editable.class,
                Component.class, mode.getClass()).invoke(null, list, list.buildEditView(), mode));
            assertNotNull(doc.selectFirst("[hx-get='/editing-demo/edit-child/module-9/item-0?editMode=" + mode + "']"));
            assertNotNull(doc.selectFirst("[hx-delete='/editing-demo/delete-child/module-9/item-0?editMode=" + mode + "']"));
            assertNotNull(doc.selectFirst("[hx-post='/editing-demo/save/module-9?editMode=" + mode + "']"));
            assertTrue(doc.text().contains("Actual child"));
        }
    }

    @Test void remainingLocalFeatureCompositionsAndChatListingMatchRenderedOutput() throws Exception {
        var templates = Jsoup.parse(FeatureExamples.render("templates"));
        var safe = Jsoup.parse(FeatureExamples.render("safe-rendering"));
        var editing = Jsoup.parse(FeatureExamples.editingExtras());
        var examples = List.of(templates.getElementById("bound-context"), templates.getElementById("live-delegates"),
            safe.getElementById("style-composition"), editing.getElementById("editablepage"));
        StringBuilder source = new StringBuilder(IMPORTS).append("public class LocalFeatures {\n");
        for (int i = 0; i < examples.size(); i++) {
            String code = examples.get(i).selectFirst(".sp-site-example-grid > .code-container code").wholeText()
                .replace("TemplateComponent.of(", "return TemplateComponent.of(")
                .replace("compiled.render(", "return compiled.render(")
                .replace("styled.render();", "return styled.render();")
                .replace("EditablePage.create(", "return EditablePage.create(");
            source.append("public static Object example").append(i).append("() {\n").append(code).append("\n}\n");
        }
        Class<?> local = compile("LocalFeatures", source.append("}\n").toString());
        for (int i = 0; i < examples.size(); i++) {
            Object result = local.getMethod("example" + i).invoke(null);
            assertEquals(normalize(examples.get(i).selectFirst("[data-example-preview]").html()),
                normalize(result instanceof Component c ? c.render() : result.toString()), examples.get(i).id());
        }
        var publication = Jsoup.parse(FeatureExamples.render("static-content")).getElementById("content-pipeline");
        String pipeline = publication.selectFirst(".code-block code").wholeText();
        String linkSource = pipeline.substring(0, pipeline.indexOf("// Application setup:"))
            .replace("\nnew Div()", "\nreturn new Div()");
        Class<?> link = compile("PublicationLink", IMPORTS + "public class PublicationLink { public static Component render() {\n" + linkSource + "\n}}\n");
        assertEquals(normalize(publication.selectFirst("[data-example-preview]").html()),
            normalize(((Component) link.getMethod("render").invoke(null)).render()));
        String chat = Jsoup.parse(FeatureExamples.chatReference()).selectFirst(".code-block code").wholeText();
        Class<?> listing = compile("ChatListing", IMPORTS + """
            import io.mindspice.demo.chat.ChatDemoService.ChatMessageView;
            import io.mindspice.simplypages.components.chat.*;
            public class ChatListing {
            """ + chat + "\n}");
        var history = List.of(new io.mindspice.demo.chat.ChatDemoService.ChatMessageView("m1", "user", "Body <&>", "Example", "12:00"));
        Component rendered = (Component) listing.getMethod("chatSpecimen", String.class, List.class).invoke(null, "example-id", history);
        String actual = Jsoup.parse(new ChatDemoPage().renderMain("example-id", history, null, false)).getElementById("chat-module").outerHtml();
        assertEquals(normalize(actual), normalize(rendered.render()));
        String forum = Jsoup.parse(FeatureExamples.forumReference()).selectFirst(".code-block code").wholeText();
        compile("ForumListing", IMPORTS + """
            import io.mindspice.simplypages.components.forum.categories.*;
            import io.mindspice.simplypages.components.forum.topics.*;
            import io.mindspice.simplypages.components.forum.actions.*;
            import io.mindspice.simplypages.components.forum.tags.*;
            public class ForumListing {
            """ + forum + "\n}");
    }

    private Class<?> compile(String name, String source) throws Exception {
        Path file = output.resolve(name + ".java");
        Files.writeString(file, source);
        var diagnostics = new java.io.ByteArrayOutputStream();
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, diagnostics, diagnostics,
            "-classpath", System.getProperty("java.class.path"), "-d", output.toString(), file.toString()), diagnostics.toString());
        return new URLClassLoader(new java.net.URL[]{output.toUri().toURL()}, getClass().getClassLoader()).loadClass(name);
    }
    private static String normalize(String html) {
        var ids = new LinkedHashMap<String, String>();
        var matcher = java.util.regex.Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matcher(html);
        return Jsoup.parseBodyFragment(matcher.replaceAll(m -> ids.computeIfAbsent(m.group(), key -> "generated-" + ids.size()))).body().html();
    }
}
