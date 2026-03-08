package io.mindspice.simplypages.core;

import io.mindspice.simplypages.testutil.HtmlAssert;
import io.mindspice.simplypages.testutil.SnapshotAssert;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
                super.withChild(new HtmlTag("h2").withClass("module-title").withInnerText(title));
            }
            if (content != null) {
                super.withChild(new HtmlTag("span").withClass("module-content").withInnerText(content));
            }
        }
    }

    @Test
    @DisplayName("Module should build once and render id class title and content structure")
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
        HtmlAssert.assertThat(html)
            .hasElement("div#module-1.featured.module")
            .childOrder("div#module-1", "h2.module-title", "span.module-content")
            .elementTextEquals("div#module-1 > h2.module-title", "Title")
            .elementTextEquals("div#module-1 > span.module-content", "Body");
        SnapshotAssert.assertMatches("core/module/title-content-classes", html);

        module.render(RenderContext.empty());
        assertEquals(1, module.buildCount);
    }

    @Test
    @DisplayName("Module should rebuild content when requested")
    void testModuleRebuild() {
        TestModule module = new TestModule();
        module.withTitle("Title");
        module.withContent("alpha");

        String initial = module.render();
        HtmlAssert.assertThat(initial)
            .hasElement("div.module > span.module-content")
            .elementTextEquals("div.module > span.module-content", "alpha");
        assertEquals(1, module.buildCount);

        module.withContent("beta");
        module.rebuildNow();
        String updated = module.render();

        assertEquals(2, module.buildCount);
        HtmlAssert.assertThat(updated)
            .hasElement("div.module > span.module-content")
            .elementTextEquals("div.module > span.module-content", "beta");
    }

    @Test
    @DisplayName("Module should reject width configuration")
    void testModuleWidthThrows() {
        TestModule module = new TestModule();

        assertThrows(UnsupportedOperationException.class, () -> module.withWidth("50%"));
        assertThrows(UnsupportedOperationException.class, () -> module.withMaxWidth("100%"));
        assertThrows(UnsupportedOperationException.class, () -> module.withMinWidth("10%"));
    }

    @Test
    @DisplayName("Module should build exactly once under concurrent first render")
    void testModuleConcurrentBuildOnce() throws Exception {
        TestModule module = new TestModule();
        module.withTitle("Concurrent");
        module.withContent("Body");
        int workers = 24;

        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);

        List<Callable<String>> tasks = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            tasks.add(() -> {
                ready.countDown();
                assertTrue(start.await(2, TimeUnit.SECONDS));
                return module.render(RenderContext.empty());
            });
        }

        List<Future<String>> futures = tasks.stream().map(pool::submit).toList();
        assertTrue(ready.await(2, TimeUnit.SECONDS));
        start.countDown();

        for (Future<String> future : futures) {
            future.get(3, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        assertEquals(1, module.buildCount);
        HtmlAssert.assertThat(module.render())
            .hasElementCount("div.module > h2.module-title", 1)
            .hasElementCount("div.module > span.module-content", 1);
    }
}
