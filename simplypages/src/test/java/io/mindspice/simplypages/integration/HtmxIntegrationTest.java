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
            .attributeEquals("button.module-edit-btn", "hx-get", "/edit/1")
            .attributeEquals("button.module-edit-btn", "hx-target", "#edit-modal-container")
            .attributeEquals("button.module-edit-btn", "hx-swap", "innerHTML")
            .attributeEquals("button.module-delete-btn", "hx-delete", "/delete/1")
            .attributeEquals("button.module-delete-btn", "hx-target", "#module-1")
            .attributeEquals("button.module-delete-btn", "hx-swap", "outerHTML")
            .childOrder("div#module-1", "button.module-edit-btn", "button.module-delete-btn", "div.content-module");
        SnapshotAssert.assertMatches("integration/htmx/editable-module-default", html);
    }
}
