package io.mindspice.simplypages.editing;

import io.mindspice.simplypages.components.Div;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditModalBuilderTest {

    @Test
    @DisplayName("EditModalBuilder should render save and delete actions")
    void testBuildWithActions() {
        String html = EditModalBuilder.create()
            .withTitle("Edit")
            .withEditView(new Div().withInnerText("Fields"))
            .withSaveUrl("/save")
            .withDeleteUrl("/delete")
            .build()
            .render();

        assertTrue(html.contains("hx-post=\"/save\""));
        assertTrue(html.contains("hx-delete=\"/delete\""));
        assertTrue(html.contains("hx-target=\"#page-content\""));
        assertTrue(html.contains("data-modal-id=\"edit-modal-container\""));
    }

    @Test
    @DisplayName("EditModalBuilder should require editView and saveUrl")
    void testRequiredFields() {
        assertThrows(IllegalStateException.class, () -> EditModalBuilder.create().build());
        assertThrows(IllegalStateException.class, () -> EditModalBuilder.create()
            .withEditView(new Div())
            .build());
    }

    @Test
    @DisplayName("EditModalBuilder should hide delete when requested")
    void testHideDelete() {
        String html = EditModalBuilder.create()
            .withEditView(new Div().withInnerText("Fields"))
            .withSaveUrl("/save")
            .withDeleteUrl("/delete")
            .hideDelete()
            .build()
            .render();

        assertTrue(html.contains("hx-post=\"/save\""));
        assertFalse(html.contains("hx-delete=\"/delete\""));
    }

    @Test
    @DisplayName("EditModalBuilder should validate container IDs")
    void testInvalidContainerIds() {
        assertThrows(IllegalArgumentException.class, () ->
            EditModalBuilder.create().withPageContainerId("1bad"));
        assertThrows(IllegalArgumentException.class, () ->
            EditModalBuilder.create().withModalContainerId("bad id"));
    }

    @Test
    @DisplayName("EditModalBuilder should use custom container IDs")
    void testCustomContainerIds() {
        String html = EditModalBuilder.create()
            .withEditView(new Div().withInnerText("Fields"))
            .withSaveUrl("/save")
            .withDeleteUrl("/delete")
            .withPageContainerId("page-root")
            .withModalContainerId("modal-root")
            .build()
            .render();

        assertTrue(html.contains("hx-target=\"#page-root\""));
        assertTrue(html.contains("data-modal-id=\"modal-root\""));
    }
}
