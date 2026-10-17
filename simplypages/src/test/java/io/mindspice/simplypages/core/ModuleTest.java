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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModuleTest {

    private static class TestModule extends Module {
        private int buildCount = 0;
        private String content;
        private Throwable failure;
        private boolean mutateExcludedState;
        private int subclassState;
        private HtmlTag descendant;
        private final List<String> externalEffects = new ArrayList<>();

        private TestModule() {
            super("div");
        }

        private TestModule withContent(String content) {
            this.content = content;
            return this;
        }

        private void failWith(Throwable failure) {
            this.failure = failure;
        }

        private void rebuildNow() {
            rebuildContent();
        }

        private void mutateExcludedStateOnFailure(HtmlTag descendant) {
            this.descendant = descendant;
            this.mutateExcludedState = true;
        }

        private List<Component> childSnapshot() {
            return List.copyOf(children);
        }

        private List<Attribute> attributeSnapshot() {
            return List.copyOf(attributes);
        }

        private String rawInnerText() {
            return innerText;
        }

        private SlotKey<String> rawInnerTextSlot() {
            return innerTextSlot;
        }

        private boolean rawTrustedHtml() {
            return trustedHtml;
        }

        private List<String> externalEffects() {
            return List.copyOf(externalEffects);
        }

        @Override
        protected void buildContent() {
            buildCount++;
            if (failure != null) {
                if (mutateExcludedState) {
                    moduleId = "mutated-module-id";
                    title = "mutated-title";
                    withInnerText("mutated plain text");
                    withInnerText(SlotKey.of("mutated-slot"));
                    withUnsafeHtml("<b>mutated trusted html</b>");
                    subclassState++;
                    descendant.withInnerText("mutated descendant");
                    externalEffects.add("external mutation");
                }
                super.withAttribute("data-mutated", "true");
                super.withId("mutated-cached-id");
                super.withChild(new HtmlTag("em").withInnerText("discarded generated child"));
                if (failure instanceof RuntimeException runtimeFailure) {
                    throw runtimeFailure;
                }
                throw (Error) failure;
            }
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
    @DisplayName("Module build should use case-insensitive class helper lookup")
    void testModuleBuildClassAlias() {
        TestModule module = new TestModule();
        module.withAttribute("CLASS", "custom");

        Template template = module.compile();
        String direct = module.render();
        assertTrue(direct.contains("class=\"custom module\""));
        assertEquals(direct, template.render(RenderContext.empty()));
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
    @DisplayName("Failed explicit rebuild restores ordered shallow entry structure and cached state")
    void testFailedRebuildRestoresOrderedShallowEntryState() {
        TestModule module = new TestModule();
        HtmlTag first = new HtmlTag("span").withInnerText("first");
        HtmlTag second = new HtmlTag("span").withInnerText("second");
        HtmlTag callerChange = new HtmlTag("span").withInnerText("caller change");
        module.withModuleId("entry-module")
            .withAttribute("data-before", "one")
            .withAttribute("data-after", "two")
            .withChild(first)
            .withChild(second);
        module.render();
        module.withAttribute("data-entry", "caller")
            .withChild(callerChange);
        List<Component> childrenBefore = module.childSnapshot();
        List<Attribute> attributesBefore = module.attributeSnapshot();
        RuntimeException failure = new IllegalStateException("rebuild failed");
        module.failWith(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class, module::rebuildNow);

        assertSame(failure, thrown);
        assertEquals(List.of(first, second, callerChange), module.childSnapshot());
        assertEquals(childrenBefore, module.childSnapshot());
        assertEquals(attributesBefore, module.attributeSnapshot());
        for (int i = 0; i < childrenBefore.size(); i++) {
            assertSame(childrenBefore.get(i), module.childSnapshot().get(i));
        }
        for (int i = 0; i < attributesBefore.size(); i++) {
            assertSame(attributesBefore.get(i), module.attributeSnapshot().get(i));
        }
        assertEquals("entry-module", module.getId());
        assertEquals(2, module.buildCount);

        String restored = module.render();
        assertEquals(restored, module.render());
        assertEquals(2, module.buildCount, "ordinary rendering must not retry a failed rebuild of a built module");
        assertTrue(restored.contains("caller change"));
    }

    @Test
    @DisplayName("Initial failed build remains retryable and preserves the same RuntimeException")
    void testInitialBuildFailureRemainsRetryable() {
        TestModule module = new TestModule().withContent("recovered");
        RuntimeException failure = new IllegalArgumentException("initial failure");
        module.failWith(failure);

        RuntimeException thrown = assertThrows(RuntimeException.class, module::render);
        assertSame(failure, thrown);
        assertEquals(1, module.buildCount);

        module.failWith(null);
        String recovered = module.render();

        assertEquals(2, module.buildCount);
        assertTrue(recovered.contains("recovered"));
    }

    @Test
    @DisplayName("Initial Error failure remains retryable")
    void testInitialErrorFailureRemainsRetryable() {
        TestModule module = new TestModule().withContent("error recovered");
        AssertionError failure = new AssertionError("initial error");
        module.failWith(failure);

        AssertionError thrown = assertThrows(AssertionError.class, module::render);
        assertSame(failure, thrown);

        module.failWith(null);
        assertTrue(module.render().contains("error recovered"));
        assertEquals(2, module.buildCount);
    }

    @Test
    @DisplayName("Error failures rethrow the same Error and restore a previously built module")
    void testErrorFailureRestoresPriorBuiltModule() {
        TestModule module = new TestModule().withContent("stable");
        String initial = module.render();
        AssertionError failure = new AssertionError("error failure");
        module.failWith(failure);

        AssertionError thrown = assertThrows(AssertionError.class, module::rebuildNow);

        assertSame(failure, thrown);
        assertEquals(initial, module.render());
        assertEquals(2, module.buildCount);
    }

    @Test
    @DisplayName("Later explicit successful rebuild replaces restored structure")
    void testLaterExplicitSuccessfulRebuildReplacesRestoredStructure() {
        TestModule module = new TestModule().withContent("before");
        HtmlTag callerChild = new HtmlTag("span").withInnerText("caller");
        module.render();
        module.withChild(callerChild);
        module.withContent("after");
        module.failWith(new IllegalStateException("temporary failure"));
        assertThrows(IllegalStateException.class, module::rebuildNow);

        module.failWith(null);
        module.rebuildNow();

        String rebuilt = module.render();
        assertEquals(3, module.buildCount);
        assertTrue(rebuilt.contains("after"));
        assertFalse(rebuilt.contains("caller"));
        assertEquals(1, module.childSnapshot().size());
    }

    @Test
    @DisplayName("Rollback is bounded to structure and leaves excluded fields and effects alone")
    void testRollbackExcludesModuleFieldsSubclassDescendantAndExternalEffects() {
        TestModule module = new TestModule();
        SlotKey<String> entrySlot = SlotKey.of("entry-slot");
        HtmlTag descendant = new HtmlTag("span").withInnerText("descendant before");
        module.withModuleId("entry-module").withTitle("entry-title").withInnerText(entrySlot);
        module.render();
        module.mutateExcludedStateOnFailure(descendant);
        module.failWith(new IllegalStateException("bounded failure"));

        assertThrows(IllegalStateException.class, module::rebuildNow);

        assertEquals("mutated-module-id", module.getModuleId());
        assertEquals("mutated-title", module.getTitle());
        assertEquals("<b>mutated trusted html</b>", module.rawInnerText());
        assertNull(module.rawInnerTextSlot());
        assertTrue(module.rawTrustedHtml());
        assertEquals(1, module.subclassState);
        assertEquals("<span>mutated descendant</span>", descendant.render());
        assertEquals(List.of("external mutation"), module.externalEffects());
    }

    @Test
    void neverBuiltExplicitRebuildFailureRestoresEntryStateAndStillRetries() {
        for (Throwable failure : List.of(new IllegalStateException("runtime"), new AssertionError("error"))) {
            TestModule module = new TestModule().withContent("recovered");
            HtmlTag caller = new HtmlTag("span").withInnerText("entry child");
            module.withChild(caller);
            module.withAttribute("CLASS", "caller").withAttribute("data-null", null).withId("entry-id");
            List<Component> children = module.childSnapshot();
            List<Attribute> attributes = module.attributeSnapshot();
            module.failWith(failure);
            assertSame(failure, assertThrows(failure.getClass(), module::rebuildNow));
            assertEquals(children, module.childSnapshot());
            assertEquals(attributes, module.attributeSnapshot());
            for (int i = 0; i < attributes.size(); i++) assertSame(attributes.get(i), module.attributeSnapshot().get(i));
            assertSame(caller, module.childSnapshot().getFirst());
            assertEquals("entry-id", module.getId());
            assertSame(failure, assertThrows(failure.getClass(), module::render));
            assertEquals(2, module.buildCount, "never-built failure must retry on ordinary rendering");
            module.failWith(null);
            assertTrue(module.render().contains("recovered"));
            assertEquals(3, module.buildCount);
            assertSame(caller, module.childSnapshot().getFirst());
        }
    }

    @Test
    void excludedTextSlotAndTrustedHtmlStateAreNotRolledBackIndividually() {
        for (String change : List.of("text", "slot", "trusted")) {
            SlotKey<String> key = SlotKey.of("excluded-" + change);
            class BoundaryModule extends Module {
                boolean fail;
                BoundaryModule() { super("section"); }
                @Override protected void buildContent() {
                    if (!fail) { withChild(new HtmlTag("span").withInnerText("preserved")); return; }
                    switch (change) {
                        case "text" -> withInnerText("changed <text>");
                        case "slot" -> withInnerText(key);
                        default -> withUnsafeHtml("<b>changed</b>");
                    }
                    throw new IllegalStateException("boundary");
                }
                void rebuild() { rebuildContent(); }
            }
            BoundaryModule module = new BoundaryModule();
            module.build();
            Component originalChild = module.children.getFirst();
            module.fail = true;
            assertThrows(IllegalStateException.class, module::rebuild);
            assertSame(originalChild, module.children.getFirst());
            String output = module.render(RenderContext.of(key, "slot <value>"));
            assertTrue(output.contains(switch (change) {
                case "text" -> "changed &lt;text&gt;";
                case "slot" -> "slot &lt;value&gt;";
                default -> "<b>changed</b>";
            }));
        }
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
