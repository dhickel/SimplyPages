package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.RichContentModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.jsoup.Jsoup;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class EditModalBuilderTest {

    private static EditModalBuilder childModal(List<EditableChild> children) {
        return EditModalBuilder.create().withSaveUrl("/save/{id}?literal=%2F#F")
                .withDeleteUrl("/delete/{id}?literal=%2F#F")
                .withEditable(new Editable<io.mindspice.simplypages.modules.ContentModule>() {
                    public Component buildEditView() { return new Paragraph("Properties"); }
                    public io.mindspice.simplypages.modules.ContentModule applyEdits(Map<String, String> data) {
                        return io.mindspice.simplypages.modules.ContentModule.create();
                    }
                    public List<EditableChild> getEditableChildren() { return children; }
                });
    }

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#encodedIds")
    void childActionsEncodeRawIdsWithoutChangingLabelsTargetsOrCompleteUrls(String raw, String encoded) {
        var document = Jsoup.parse(childModal(List.of(EditableChild.create(raw, "<Child>", new Paragraph("Body"))))
                .withChildEditUrl("/edit/{id}?again={id}#F?")
                .withChildDeleteUrl("/delete?child={id}&again={id}#F?")
                .withModalContainerId("custom-host").withPageContainerId("custom-page").build().render());
        var item = document.selectFirst(".list-group-item");
        assertEquals("<Child>", item.child(0).text());
        var edit = item.selectFirst("[hx-get]");
        assertEquals("/edit/" + encoded + "?again=" + encoded + "#F?", edit.attr("hx-get"));
        assertEquals("#custom-host", edit.attr("hx-target"));
        assertEquals("innerHTML", edit.attr("hx-swap"));
        var delete = item.selectFirst("[hx-delete]");
        assertEquals("/delete?child=" + encoded + "&again=" + encoded + "#F?", delete.attr("hx-delete"));
        assertEquals("#custom-host", delete.attr("hx-target"));
        assertEquals("Delete this item?", delete.attr("hx-confirm"));
        assertFalse(delete.hasAttr("hx-swap"));
        assertEquals("/save/{id}?literal=%2F#F", document.selectFirst("[hx-post]").attr("hx-post"));
        assertEquals("/delete/{id}?literal=%2F#F", document.selectFirst(".modal-footer [hx-delete]").attr("hx-delete"));
        assertEquals("#custom-page", document.selectFirst(".modal-footer [hx-delete]").attr("hx-target"));
    }

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#invalidIds")
    void childDescriptorIsNotGloballyValidated(String raw) {
        var child = assertDoesNotThrow(() -> EditableChild.create(raw, "Label", null));
        assertEquals(raw, child.getId());
        assertDoesNotThrow(() -> childModal(List.of(child)).withChildEditUrl(null).withChildDeleteUrl(null).build());
        var fixed = Jsoup.parse(childModal(List.of(child)).withChildEditUrl("/fixed-edit")
                .withChildDeleteUrl("/fixed-delete").build().render());
        assertEquals("/fixed-edit", fixed.selectFirst(".edit-children-section [hx-get]").attr("hx-get"));
        assertEquals("/fixed-delete", fixed.selectFirst(".edit-children-section [hx-delete]").attr("hx-delete"));
        assertThrows(IllegalArgumentException.class, () -> childModal(List.of(child)).withChildEditUrl("/{id}").build());
        assertThrows(IllegalArgumentException.class, () -> childModal(List.of(child)).withChildDeleteUrl("/{id}").build());
        assertDoesNotThrow(() -> childModal(List.of()).withChildEditUrl("/{id}").withChildDeleteUrl("/{id}").build());
    }

    @Test
    void rejectsLegacyPartialPlacementsAndUnknownTokensAtConfiguration() {
        var modal = childModal(List.of());
        for (String template : List.of("/child-{id}", "/{id}.json", "/?{id}=x", "/?x=pre{id}",
                "https://{id}/edit", "/edit#{id}", "/{pageId}", "/{id")) {
            assertThrows(IllegalArgumentException.class, () -> modal.withChildEditUrl(template));
            assertThrows(IllegalArgumentException.class, () -> modal.withChildDeleteUrl(template));
        }
    }

    @ParameterizedTest
    @MethodSource("io.mindspice.simplypages.editing.EditingUrlTemplateTest#authorityCounterexamples")
    void rejectsBrowserAuthorityPlacementsWithoutReplacingChildTemplates(String template) {
        var modal = childModal(List.of(EditableChild.create("destination.example", "Child", null)))
                .withChildEditUrl("/children/{id}/edit").withChildDeleteUrl("/children/{id}/delete");
        assertThrows(IllegalArgumentException.class, () -> modal.withChildEditUrl(template));
        assertThrows(IllegalArgumentException.class, () -> modal.withChildDeleteUrl(template));
        var document = Jsoup.parse(modal.build().render());
        assertEquals("/children/destination.example/edit",
                document.selectFirst(".edit-children-section [hx-get]").attr("hx-get"));
        assertEquals("/children/destination.example/delete",
                document.selectFirst(".edit-children-section [hx-delete]").attr("hx-delete"));
    }

    @Test
    void testNestedEditingStructure() {
        RichContentModule module = new RichContentModule("Test Module");
        module.addParagraph(new Paragraph("Hello"));
        module.addParagraph(new Paragraph("World"));

        Modal modal = EditModalBuilder.create()
                .withTitle("Edit Test")
                .withEditable(module)
                .withSaveUrl("/save")
                .withDeleteUrl("/delete")
                .withChildEditUrl("/edit-child/{id}")
                .build();

        String html = modal.render();

        var document = org.jsoup.Jsoup.parse(html);
        var dialog = document.selectFirst(".modal-container");
        assertEquals("dialog", dialog.attr("role"));
        assertEquals("true", dialog.attr("aria-modal"));
        assertEquals("Edit Test", document.getElementById(dialog.attr("aria-labelledby")).text());
        assertEquals("false", document.selectFirst(".modal-backdrop").attr("data-close-on-backdrop"));
        assertEquals("true", document.selectFirst(".modal-backdrop").attr("data-close-on-escape"));
        assertEquals(1, document.select("button.modal-close[aria-label=Close]").size());

        // Check if main properties form is present (title field)
        assertTrue(html.contains("name=\"title\""), "Should contain title field");

        // Check if children section is present
        assertTrue(html.contains("Content Items"), "Should contain 'Content Items' header");

        // Check if children are listed
        assertTrue(html.contains("Paragraph 1"), "Should list Paragraph 1");
        assertTrue(html.contains("Paragraph 2"), "Should list Paragraph 2");

        // Check if child edit buttons are present with correct URLs
        assertTrue(html.contains("hx-get=\"/edit-child/child-0\""), "Should have edit link for child 0");
        assertTrue(html.contains("hx-get=\"/edit-child/child-1\""), "Should have edit link for child 1");
    }

    @Test
    void testChildUrlsEncodePathSegments() {
        Editable<ContentModule> editable = new Editable<>() {
            @Override
            public Component buildEditView() {
                return new Div();
            }

            @Override
            public java.util.List<EditableChild> getEditableChildren() {
                return java.util.List.of(EditableChild.create("item 1/2", "Unsafe ID", new Div()));
            }

            @Override
            public ContentModule applyEdits(java.util.Map<String, String> formData) {
                return ContentModule.create();
            }
        };

        String html = EditModalBuilder.create()
                .withTitle("Edit Test")
                .withEditable(editable)
                .withSaveUrl("/save")
                .withChildEditUrl("/edit-child/{id}")
                .withChildDeleteUrl("/delete-child/{id}")
                .build()
                .render();

        assertTrue(html.contains("hx-get=\"/edit-child/item%201%2F2\""));
        assertTrue(html.contains("hx-delete=\"/delete-child/item%201%2F2\""));
    }

    @Test
    void testModuleIdRemainsAnOpaqueDescriptor() {
        var builder = EditModalBuilder.create().withModuleId("module/1")
                .withEditView(new Div()).withSaveUrl("/save");
        assertEquals("/save", org.jsoup.Jsoup.parse(builder.build().render())
                .selectFirst("button[hx-post]").attr("hx-post"));
    }

    @Test
    void customContainersRetainSaveAndExternalCloseWiring() {
        var document = org.jsoup.Jsoup.parse(EditModalBuilder.create()
            .withTitle(null).withEditView(new Paragraph("Properties")).withSaveUrl("/save")
            .withModalContainerId("custom-modal-host").withPageContainerId("custom-page")
            .hideDelete().build().render());
        assertEquals("Dialog", document.selectFirst("[role=dialog]").attr("aria-label"));
        var close = document.selectFirst(".modal-footer button[data-modal-id]");
        assertEquals("custom-modal-host", close.attr("data-modal-id"));
        assertEquals("document.getElementById(this.dataset.modalId).innerHTML = ''", close.attr("onclick"));
        assertEquals("none", document.selectFirst("button[hx-post]").attr("hx-swap"));
        assertTrue(document.select("button[hx-delete]").isEmpty());
    }
}
