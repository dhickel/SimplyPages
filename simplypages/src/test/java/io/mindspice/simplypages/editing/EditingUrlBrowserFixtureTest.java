package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.modules.ContentModule;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Writes fresh actual framework output consumed by the URI-normalization browser regressions. */
class EditingUrlBrowserFixtureTest {
    @Test
    void generatesActualEditingMarkupAndRejectsDotIdsBeforeEmittingControls() throws Exception {
        Div fixture = new Div().withId("editing-url-fixture");
        for (String id : List.of("tenant/a", "a%2Fb", "%2e%2E", "back\\slash?hash#", " café 😀 ", "{pageId}")) {
            Div sample = new Div().withClass("url-sample").withAttribute("data-raw-id", id);
            sample.withChild(EditablePage.create(id));
            sample.withChild(EditableRow.wrap(null, id, id).withEditMode(null)
                    .withModuleEditUrlTemplate("/edit/{pageId}/{rowId}/{moduleId}?id={moduleId}#fragment?")
                    .withModuleDeleteUrlTemplate("/delete/{pageId}/{rowId}/{moduleId}?id={moduleId}#fragment?")
                    .addEditableModule(ContentModule.create().withContent("Body"), id));
            sample.withChild(EditModalBuilder.create().withSaveUrl("/save")
                    .withEditable(new Editable<ContentModule>() {
                        public Component buildEditView() { return new Paragraph("Properties"); }
                        public ContentModule applyEdits(Map<String, String> data) { return ContentModule.create(); }
                        public List<EditableChild> getEditableChildren() {
                            return List.of(EditableChild.create(id, "Child", null));
                        }
                    }).withChildEditUrl("/child/{id}?id={id}#fragment?")
                    .withChildDeleteUrl("/child-delete/{id}?id={id}#fragment?").build());
            fixture.withChild(sample);
        }
        for (String id : List.of(".", "..")) {
            assertThrows(IllegalArgumentException.class, () -> EditablePage.create(id).render());
            assertThrows(IllegalArgumentException.class, () -> EditableRow.wrap(null, id, id)
                    .addEditableModule(ContentModule.create(), id).render());
            // Rejected input does not produce a URL; the marker records the Java assertion above.
            fixture.withChild(new Div().withClass("rejected-id").withAttribute("data-raw-id", id));
        }
        for (String template : EditingUrlTemplateTest.authorityCounterexamples().toList()) {
            var page = EditablePage.create("destination.example");
            assertThrows(IllegalArgumentException.class,
                    () -> page.withInsertRowUrlTemplate(template.replace("{id}", "{pageId}")));
            // This is hypothetical rejected text, NOT a generated action URL. Base64 retains
            // C0 controls faithfully across HTML parsing for the browser-normalization probe.
            fixture.withChild(new Div().withClass("rejected-authority-template")
                    .withAttribute("data-candidate-base64", Base64.getEncoder().encodeToString(
                            template.replace("{id}", "destination.example").getBytes(StandardCharsets.UTF_8))));
        }
        String[][] supported = {
                {"https://literal.example/{pageId}/edit?id={pageId}#F?", "literal.example", "/{id}/edit"},
                {"//literal.example/{pageId}/edit?id={pageId}#F?", "literal.example", "/{id}/edit"},
                {"/{pageId}/edit?id={pageId}#F?", "127.0.0.1", "/{id}/edit"},
                {"{pageId}/edit?id={pageId}#F?", "127.0.0.1", "/{id}/edit"},
                {"./{pageId}/edit?id={pageId}#F?", "127.0.0.1", "/{id}/edit"},
                {"../{pageId}/edit?id={pageId}#F?", "127.0.0.1", "/{id}/edit"},
                {"?id={pageId}#F?", "127.0.0.1", "/editing-url-fixture"},
                {"https://literal.example?id={pageId}#F?", "literal.example", "/"},
                {"//literal.example?id={pageId}#F?", "literal.example", "/"},
                {" https://literal.example/{pageId}/edit?id={pageId}#F?", "literal.example", "/{id}/edit"},
                {"h\tttps://literal.example/{pageId}/edit?id={pageId}#F?", "literal.example", "/{id}/edit"},
                {"https:\t//literal.example/{pageId}/edit?id={pageId}#F?", "literal.example", "/{id}/edit"},
                {"https:/\n/literal.example/{pageId}/edit?id={pageId}#F?", "literal.example", "/{id}/edit"},
                {"https:\\\\literal.example/{pageId}/edit?id={pageId}#F?", "literal.example", "/{id}/edit"},
                {"/folder\\/{pageId}/edit?id={pageId}#F?", "127.0.0.1", "/folder//{id}/edit"},
                {"/root/{pageId}/edit?id={pageId}&literal=\\#F?", "127.0.0.1", "/root/{id}/edit"}
        };
        for (String id : List.of("destination.example", "tenant/a%")) {
            for (String[] grammar : supported) {
                var page = EditablePage.create(id).withInsertRowUrlTemplate(grammar[0]);
                fixture.withChild(new Div().withClass("supported-template")
                        .withAttribute("data-raw-id", id).withAttribute("data-expected-host", grammar[1])
                        .withAttribute("data-expected-path", grammar[2]).withChild(page));
            }
        }
        String html = fixture.render();
        var document = Jsoup.parse(html);
        assertEquals(6, document.select(".url-sample").size());
        assertEquals(52, document.select(".rejected-authority-template").size());
        assertEquals(32, document.select(".supported-template [hx-post]").size());
        assertTrue(document.select(".rejected-authority-template [hx-get], .rejected-authority-template [hx-post],"
                + " .rejected-authority-template [hx-delete]").isEmpty());
        Path output = Path.of("target", "editing-url-browser-fixture.html");
        Files.createDirectories(output.getParent());
        Files.writeString(output, html);
    }
}
