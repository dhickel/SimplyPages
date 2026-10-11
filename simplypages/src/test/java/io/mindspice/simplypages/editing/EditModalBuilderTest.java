package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.display.Modal;
import io.mindspice.simplypages.modules.RichContentModule;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EditModalBuilderTest {

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
