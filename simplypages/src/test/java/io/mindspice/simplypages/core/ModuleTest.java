package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleTest {

    private static class TestModule extends Module {
        private int buildCount = 0;
        private String content;

        private TestModule() {
            super("div");
        }

        private TestModule withContent(String content) {
            this.content = content;
            return this;
        }

        private void rebuildNow() {
            rebuildContent();
        }

        @Override
        protected void buildContent() {
            buildCount++;
            if (title != null) {
                super.withChild(new HtmlTag("h2").withInnerText(title));
            }
            if (content != null) {
                super.withChild(new HtmlTag("span").withInnerText(content));
            }
        }
    }

    @Test
    @DisplayName("Module should build once and render with module class")
    void testModuleBuildAndRender() {
        TestModule module = new TestModule();
        module.withTitle("Title");
        module.withContent("Body");
        module.withClass("featured");
        module.withModuleId("module-1");

        String html = module.render();

        assertEquals("module-1", module.getModuleId());
        assertEquals("Title", module.getTitle());
        assertEquals(1, module.buildCount);
        assertTrue(html.contains("class=\"featured module\""));
        assertTrue(html.contains("id=\"module-1\""));
        assertTrue(html.contains(">Title</h2>"));
        assertTrue(html.contains(">Body</span>"));

        module.render(RenderContext.empty());
        assertEquals(1, module.buildCount);
    }

    @Test
    @DisplayName("Module should rebuild content when requested")
    void testModuleRebuild() {
        TestModule module = new TestModule();
        module.withTitle("Title");
        module.withContent("alpha");

        String html = module.render();
        assertTrue(html.contains(">alpha</span>"));
        assertEquals(1, module.buildCount);

        module.withContent("beta");
        module.rebuildNow();
        String updated = module.render();

        assertEquals(2, module.buildCount);
        assertTrue(updated.contains(">beta</span>"));
        assertFalse(updated.contains(">alpha</span>"));
    }

    @Test
    @DisplayName("Module should reject width configuration")
    void testModuleWidthThrows() {
        TestModule module = new TestModule();

        assertThrows(UnsupportedOperationException.class, () -> module.withWidth("50%"));
        assertThrows(UnsupportedOperationException.class, () -> module.withMaxWidth("100%"));
        assertThrows(UnsupportedOperationException.class, () -> module.withMinWidth("10%"));
    }
}
