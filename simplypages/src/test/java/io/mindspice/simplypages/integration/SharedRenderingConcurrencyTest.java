package io.mindspice.simplypages.integration;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.components.content.ContentRouteIndex;
import io.mindspice.simplypages.components.content.ContentSectionConfig;
import io.mindspice.simplypages.components.content.ContentSiteBundle;
import io.mindspice.simplypages.components.content.StaticContentSite;
import io.mindspice.simplypages.components.content.StaticContentSiteBuilder;
import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.core.RenderContext;
import io.mindspice.simplypages.core.Slot;
import io.mindspice.simplypages.core.SlotKey;
import io.mindspice.simplypages.core.Template;
import io.mindspice.simplypages.core.TemplateComponent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedRenderingConcurrencyTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Shared Template should isolate per-request RenderContext values under concurrency")
    void sharedTemplateIsolatesPerRequestContexts() throws Exception {
        SlotKey<String> title = SlotKey.of("title");
        SlotKey<String> body = SlotKey.of("body");
        Template template = Template.of(new Div().withClass("card")
            .withChild(Header.H2("").withClass("title").withInnerText(title))
            .withChild(new Paragraph().withClass("body").withInnerText(body)));

        runConcurrent(12, 240, index -> {
            String titleValue = "Title " + index;
            String bodyValue = "Body " + index;
            RenderContext context = RenderContext.builder()
                .with(title, titleValue)
                .with(body, bodyValue)
                .build();

            String html = template.render(context);
            assertTrue(html.contains(titleValue));
            assertTrue(html.contains(bodyValue));
            assertFalse(html.contains("Title " + (index + 1)));
            return null;
        });
    }

    @Test
    @DisplayName("Shared Template should cache component slots only inside each request context")
    void sharedTemplateCompileOnFirstHitDoesNotCrossContexts() throws Exception {
        SlotKey<Component> dynamic = SlotKey.of("dynamic");
        Template template = Template.of(new Div().withClass("host").withChild(Slot.of(dynamic)));

        runConcurrent(10, 200, index -> {
            RenderContext context = RenderContext.empty()
                .withPolicy(RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT)
                .put(dynamic, new Paragraph("Value " + index).withClass("value"));

            String first = template.render(context);
            String second = template.render(context);

            assertEquals(first, second);
            assertTrue(context.isCompiled(dynamic));
            assertTrue(first.contains("Value " + index));
            assertFalse(first.contains("Value " + (index + 1)));
            return null;
        });
    }

    @Test
    @DisplayName("TemplateComponent with stable bound context should render concurrently")
    void templateComponentWithStableContextRendersConcurrently() throws Exception {
        SlotKey<String> label = SlotKey.of("label");
        Template template = Template.of(new Div().withClass("stable")
            .withChild(new Paragraph().withInnerText(label)));
        TemplateComponent component = TemplateComponent.of(template, RenderContext.of(label, "Shared Label"));

        runConcurrent(8, 160, index -> {
            String html = component.render(RenderContext.empty());
            assertTrue(html.contains("Shared Label"));
            assertFalse(html.contains("Request " + index));
            return null;
        });
    }

    @Test
    @DisplayName("Generated static content route index should resolve and render concurrently")
    void staticContentRouteIndexRendersConcurrently() throws Exception {
        Path blogs = tempDir.resolve("blogs");
        write(blogs.resolve("alpha.md"), """
            ---
            title: Alpha
            date: 2026-05-20
            ---
            # Alpha
            Alpha body.
            """);
        write(blogs.resolve("bravo.md"), """
            ---
            title: Bravo
            date: 2026-05-21
            ---
            # Bravo
            Bravo body.
            """);

        StaticContentSite site = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("blogs", blogs, "/blogs").withPageSize(1))
            .build();
        ContentSiteBundle bundle = site.generate();
        ContentRouteIndex routes = bundle.routeIndex();

        runConcurrent(8, 160, index -> {
            String route = index % 2 == 0 ? "/blogs" : "/blogs?page=2";
            String html = routes.resolveRoute(route).orElseThrow().render(RenderContext.empty());

            if (index % 2 == 0) {
                assertTrue(html.contains("Bravo"));
                assertFalse(html.contains("Alpha body."));
            } else {
                assertTrue(html.contains("Alpha"));
                assertFalse(html.contains("Bravo body."));
            }
            return null;
        });
    }

    private void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, content, StandardCharsets.UTF_8);
    }

    private void runConcurrent(int threads, int iterations, ConcurrentAssertion assertion) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < iterations; i++) {
            int index = i;
            futures.add(executor.submit((Callable<Void>) () -> {
                assertTrue(start.await(5, TimeUnit.SECONDS), "concurrent test start timed out");
                return assertion.run(index);
            }));
        }

        start.countDown();
        for (Future<Void> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "executor did not terminate");
    }

    @FunctionalInterface
    private interface ConcurrentAssertion {
        Void run(int index) throws Exception;
    }
}
