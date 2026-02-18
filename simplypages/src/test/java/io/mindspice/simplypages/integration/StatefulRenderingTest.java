package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Slot;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import io.mindspice.simplypages.modules.ContentModule;
import io.mindspice.simplypages.modules.EditableModule;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatefulRenderingTest {

    @Test
    @DisplayName("Editable module should remain structurally stable across repeated renders and edits")
    void testEditableModuleRenderLifecycleStability() {
        ContentModule content = ContentModule.create()
            .withModuleId("content-1")
            .withTitle("Initial")
            .withContent("Initial body");

        EditableModule editable = EditableModule.wrap(content)
            .withModuleId("editable-1")
            .withEditUrl("/edit/1")
            .withDeleteUrl("/delete/1");

        String initialHtml = editable.render();
        String secondHtml = editable.render();

        HtmlAssert.assertThat(initialHtml)
            .hasElementCount("#editable-1 > button.module-edit-btn", 1)
            .hasElementCount("#editable-1 > button.module-delete-btn", 1)
            .hasElementCount("#editable-1 > div.content-module", 1);

        HtmlAssert.assertThat(secondHtml)
            .hasElementCount("#editable-1 > button.module-edit-btn", 1)
            .hasElementCount("#editable-1 > button.module-delete-btn", 1)
            .hasElementCount("#editable-1 > div.content-module", 1);

        content.applyEdits(Map.of(
            "title", "Updated",
            "content", "Updated body"
        ));

        String updatedHtml = editable.render();

        HtmlAssert.assertThat(updatedHtml)
            .hasElementCount("#editable-1 > button.module-edit-btn", 1)
            .hasElementCount("#editable-1 > button.module-delete-btn", 1)
            .hasElementCount("#editable-1 > div.content-module", 1)
            .elementTextEquals("#editable-1 > div.content-module > h2.module-title", "Updated")
            .elementTextEquals("#editable-1 > div.content-module div.module-content", "Updated body");
    }

    @Test
    @DisplayName("Template should compile once, then recompile only after context mutation")
    void testRenderContextPolicyAcrossRenderStages() {
        SlotKey<Component> key = SlotKey.of("dynamic");
        Template template = Template.of(new Div().withClass("template-host").withChild(Slot.of(key)));

        AtomicInteger renderCounter = new AtomicInteger(0);
        RenderContext context = RenderContext.empty()
            .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT)
            .put(key, new CountingComponent(renderCounter, "phase-1"));

        String phaseOne = template.render(context);
        String phaseOneRepeat = template.render(context);

        HtmlAssert.assertThat(phaseOne).elementTextEquals("div.template-host > span.dynamic", "phase-1");
        HtmlAssert.assertThat(phaseOneRepeat).elementTextEquals("div.template-host > span.dynamic", "phase-1");
        assertEquals(1, renderCounter.get());
        assertTrue(context.isCompiled(key));

        context.put(key, new CountingComponent(renderCounter, "phase-2"));

        String phaseTwo = template.render(context);

        HtmlAssert.assertThat(phaseTwo).elementTextEquals("div.template-host > span.dynamic", "phase-2");
        assertEquals(2, renderCounter.get());
        assertTrue(context.isCompiled(key));
    }

    @Test
    @DisplayName("Content module should avoid duplicate structural wrappers across multi-stage edit flow")
    void testContentModuleNoDuplicateWrappersAcrossEdits() {
        ContentModule module = ContentModule.create()
            .withTitle("Baseline")
            .withContent("One");

        String baseline = module.render();

        module.applyEdits(Map.of("title", "Revision A", "content", "Two"));
        String revisionA = module.render();

        module.applyEdits(Map.of("title", "Revision B", "content", "Three", "useMarkdown", "on"));
        String revisionB = module.render();

        HtmlAssert.assertThat(baseline)
            .hasElementCount("div.content-module > h2.module-title", 1)
            .hasElementCount("div.content-module > div.module-content", 1);

        HtmlAssert.assertThat(revisionA)
            .hasElementCount("div.content-module > h2.module-title", 1)
            .hasElementCount("div.content-module > div.module-content", 1)
            .elementTextEquals("div.content-module > h2.module-title", "Revision A");

        HtmlAssert.assertThat(revisionB)
            .hasElementCount("div.content-module > h2.module-title", 1)
            .hasElementCount("div.content-module > div.module-content", 1)
            .elementTextEquals("div.content-module > h2.module-title", "Revision B");
    }

    private static class CountingComponent implements Component {
        private final AtomicInteger renderCounter;
        private final String label;

        private CountingComponent(AtomicInteger renderCounter, String label) {
            this.renderCounter = renderCounter;
            this.label = label;
        }

        @Override
        public String render(RenderContext context) {
            renderCounter.incrementAndGet();
            return "<span class=\"dynamic\">" + label + "</span>";
        }
    }
}
