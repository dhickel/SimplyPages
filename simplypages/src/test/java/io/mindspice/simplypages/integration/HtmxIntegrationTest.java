package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.EditableModule;
import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class HtmxIntegrationTest {

    @Test
    @DisplayName("EditableModule should render HTMX edit/delete attributes")
    void testEditableModuleHtmxAttributes() {
        ContentModule content = ContentModule.create()
            .withTitle("Title")
            .withContent("Body");

        EditableModule editable = EditableModule.wrap(content)
            .withModuleId("module-1")
            .withEditUrl("/edit/1")
            .withDeleteUrl("/delete/1");

        String html = editable.render();

        HtmlAssert.assertThat(html)
            .hasElement("div#module-1.editable-module-wrapper")
            .hasElement("div#module-1 > button.module-edit-btn")
            .hasElement("div#module-1 > button.module-delete-btn")
            .hasElement("div#module-1 > div.content-module")
            .doesNotHaveElement("div#module-1[hx-get]")
            .doesNotHaveElement("div#module-1 > div.content-module[hx-get]")
            .doesNotHaveElement("div#module-1 > button.module-edit-btn[hx-delete]")
            .doesNotHaveElement("div#module-1 > button.module-delete-btn[hx-get]")
            .attributeEquals("button.module-edit-btn", "hx-get", "/edit/1")
            .attributeEquals("button.module-edit-btn", "hx-target", "#edit-modal-container")
            .attributeEquals("button.module-edit-btn", "hx-swap", "innerHTML")
            .attributeEquals("button.module-delete-btn", "hx-delete", "/delete/1")
            .attributeEquals("button.module-delete-btn", "hx-target", "#module-1")
            .attributeEquals("button.module-delete-btn", "hx-swap", "outerHTML")
            .childOrder("div#module-1", "button.module-edit-btn", "button.module-delete-btn", "div.content-module");
        SnapshotAssert.assertMatches("integration/htmx/editable-module-default", html);
    }

    @Test
    @DisplayName("EditableModule should append editMode query parameter to HTMX endpoints")
    void testEditableModuleHtmxEditModeEndpoints() {
        ContentModule content = ContentModule.create()
            .withTitle("Title")
            .withContent("Body");

        EditableModule editable = EditableModule.wrap(content)
            .withModuleId("module-2")
            .withEditUrl("/edit/2")
            .withDeleteUrl("/delete/2")
            .withEditMode(io.mindspice.simplypages.editing.EditMode.OWNER_EDIT);

        String html = editable.render();

        HtmlAssert.assertThat(html)
            .attributeEquals("button.module-edit-btn", "hx-get", "/edit/2?editMode=OWNER_EDIT")
            .attributeEquals("button.module-delete-btn", "hx-delete", "/delete/2?editMode=OWNER_EDIT")
            .attributeEquals("button.module-delete-btn", "hx-target", "#module-2")
            .childOrder("div#module-2", "button.module-edit-btn", "button.module-delete-btn", "div.content-module");
    }
}
