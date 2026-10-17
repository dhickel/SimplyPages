package io.mindspice.simplypages.components.content;

import io.mindspice.simplypages.core.Component;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ContentRouteCollisionTest {
    @TempDir Path root;

    @ParameterizedTest
    @CsvSource({"/, false", "/, true", "/articles, false", "/articles, true"})
    void publishedDetailShadowsIndexWithoutLosingPages(String base, boolean indexFirst) throws IOException {
        Path detailDirectory = directory("detail");
        Path indexDirectory = directory("index");
        Path source = write(detailDirectory, "foo.md", "# Detail winner\n\n## Visible heading\nBody.");
        write(indexDirectory, "a.md", "# Index A");
        write(indexDirectory, "b.md", "# Index B");
        String route = (base.equals("/") ? "" : base) + "/foo";
        ContentSectionConfig detailConfig = ContentSectionConfig.create("detail-owner", detailDirectory, base);
        // Exercise the existing base-path normalization, not a new alias policy.
        ContentSectionConfig indexConfig = ContentSectionConfig.create("index-owner", indexDirectory, route + "/")
            .withPageSize(1);
        StaticContentSiteBuilder builder = StaticContentSiteBuilder.create();
        if (indexFirst) {
            builder.addSection(indexConfig).addSection(detailConfig);
        } else {
            builder.addSection(detailConfig).addSection(indexConfig);
        }
        ContentSiteBundle bundle = builder.build().generate();
        ContentRouteIndex routes = bundle.routeIndex();
        assertEquals(1, bundle.warnings().size());
        assertCollision(bundle.warnings().getFirst(), route, "detail-owner", source, "index-owner", indexDirectory);

        ContentSectionSite detailSection = bundle.sectionsByKey().get("detail-owner");
        ContentSectionSite indexSection = bundle.sectionsByKey().get("index-owner");
        assertEquals(route, detailSection.entries().getFirst().route());
        Component detail = detailSection.detailPagesBySlug().get("foo");
        assertSame(detail, routes.resolveDetail("detail-owner", "foo").orElseThrow());
        assertSame(detail, routes.detailPagesByRoute().get(route));
        for (String path : List.of(route, route + "/")) {
            for (String page : List.of("1", "2")) {
                assertSame(detail, routes.resolveRequest(path, page).orElseThrow());
                assertSame(detail, routes.resolveRoute(path + "?page=" + page).orElseThrow());
            }
            assertSame(detail, routes.resolveRoute(path).orElseThrow());
        }
        for (int page = 1; page <= 2; page++) {
            Component index = indexSection.indexPagesByPage().get(page);
            assertSame(index, routes.resolveIndex("index-owner", page).orElseThrow());
            assertSame(index, routes.indexPagesByRoute().get(routes.indexRoute("index-owner", page)));
            assertNotSame(detail, index);
            assertEquals(1, Jsoup.parse(index.render()).select(".sp-content-list-item").size());
        }
        assertEquals(route + "?page=2", Jsoup.parse(routes.resolveIndex("index-owner", 1).orElseThrow().render())
            .selectFirst("a.sp-content-page-next").attr("href"));
        assertEquals("Detail winner", Jsoup.parse(detail.render()).selectFirst("h1").text());
        assertThrows(UnsupportedOperationException.class, () -> bundle.warnings().clear());
        assertThrows(UnsupportedOperationException.class, () -> bundle.warnings().add(bundle.warnings().getFirst()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void repeatedGenerationHasOneWarningPerPairInConfigurationAndEntryOrder(boolean reverse) throws IOException {
        Path first = directory("first");
        Path second = directory("second");
        Path firstA = write(first, "a.md", "# A");
        Path firstB = write(first, "b.md", "# B");
        Path secondA = write(second, "a.md", "# A");
        Path firstAIndex = directory("first-a-index");
        Path firstBIndex = directory("first-b-index");
        Path secondAIndex = directory("second-a-index");
        var builder = StaticContentSiteBuilder.create();
        ContentSectionConfig firstConfig = ContentSectionConfig.create("first", first, "/first");
        ContentSectionConfig secondConfig = ContentSectionConfig.create("second", second, "/second");
        if (reverse) {
            builder.addSection(secondConfig).addSection(firstConfig);
        } else {
            builder.addSection(firstConfig).addSection(secondConfig);
        }
        StaticContentSite site = builder
            .addSection(ContentSectionConfig.create("first-a-index", firstAIndex, "/first/a"))
            .addSection(ContentSectionConfig.create("first-b-index", firstBIndex, "/first/b"))
            .addSection(ContentSectionConfig.create("second-a-index", secondAIndex, "/second/a"))
            .build();
        ContentSiteBundle bundle = site.generate();
        List<Path> expected = reverse ? List.of(secondA, firstA, firstB) : List.of(firstA, firstB, secondA);
        assertEquals(expected, bundle.warnings().stream().map(ContentWarning::sourcePath).toList());
        assertEquals(3, bundle.warnings().size());
        for (int i = 0; i < 4; i++) {
            ContentSiteBundle regenerated = site.generate();
            assertEquals(bundle.warnings(), regenerated.warnings());
            assertEquals(bundle.routeIndex().indexPagesByRoute().keySet(), regenerated.routeIndex().indexPagesByRoute().keySet());
            assertEquals(bundle.routeIndex().detailPagesByRoute().keySet(), regenerated.routeIndex().detailPagesByRoute().keySet());
        }
        // Empty sections still generate an index and therefore are real collision owners.
        assertEquals(1, bundle.sectionsByKey().get("first-a-index").indexPagesByPage().size());
    }

    @Test
    void diagnosticsUseFinalUniquifiedRoutesAndRetainExistingWarnings() throws IOException {
        Path detail = directory("detail");
        Path a = write(detail, "a.md", "---\nslug: foo\ndate: invalid\n---\n# First");
        Path b = write(detail, "b.md", "---\nslug: foo\n---\n# Second");
        Path c = write(detail, "c.md", "---\nslug: foo-2\n---\n# Third");
        Path original = directory("original");
        Path adjusted = directory("adjusted");
        Path suffixed = directory("suffixed");
        ContentSiteBundle bundle = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("detail", detail, "/"))
            .addSection(ContentSectionConfig.create("original", original, "/foo"))
            .addSection(ContentSectionConfig.create("adjusted", adjusted, "/foo-2"))
            .addSection(ContentSectionConfig.create("suffixed", suffixed, "/foo-2-2"))
            .build().generate();
        assertEquals(List.of("/foo", "/foo-2", "/foo-2-2"), bundle.sectionsByKey().get("detail").entries()
            .stream().map(ContentEntryRecord::route).toList());
        assertEquals(6, bundle.warnings().size());
        assertTrue(bundle.warnings().get(0).message().startsWith("Invalid date format"));
        assertEquals(a, bundle.warnings().get(0).sourcePath());
        assertTrue(bundle.warnings().get(1).message().contains("adjusted to 'foo-2'"));
        assertTrue(bundle.warnings().get(2).message().contains("adjusted to 'foo-2-2'"));
        assertCollision(bundle.warnings().get(3), "/foo", "detail", a, "original", original);
        assertCollision(bundle.warnings().get(4), "/foo-2", "detail", b, "adjusted", adjusted);
        assertCollision(bundle.warnings().get(5), "/foo-2-2", "detail", c, "suffixed", suffixed);
    }

    @Test
    void draftsDoNotReserveSlugsOrCreateFalseCollisions() throws IOException {
        Path detail = directory("detail");
        write(detail, "a-draft.md", "---\nslug: foo\ndraft: true\n---\n# Draft");
        Path published = write(detail, "b-public.md", "---\nslug: foo\n---\n# Published");
        write(detail, "c-draft.md", "---\nslug: hidden\ndraft: true\n---\n# Hidden");
        Path index = directory("index");
        ContentSiteBundle bundle = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("detail", detail, "/"))
            .addSection(ContentSectionConfig.create("index", index, "/foo"))
            .addSection(ContentSectionConfig.create("not-adjusted", directory("not-adjusted"), "/foo-2"))
            .addSection(ContentSectionConfig.create("hidden", directory("hidden"), "/hidden"))
            .addSection(ContentSectionConfig.create("ordinary", directory("ordinary"), "/ordinary"))
            .build().generate();
        assertEquals(1, bundle.warnings().size());
        assertCollision(bundle.warnings().getFirst(), "/foo", "detail", published, "index", index);
        assertEquals(1, bundle.sectionsByKey().get("detail").entries().size());
        assertFalse(bundle.routeIndex().resolveDetail("detail", "foo-2").isPresent());
        assertFalse(bundle.routeIndex().resolveDetail("detail", "hidden").isPresent());
        for (String key : List.of("not-adjusted", "hidden", "ordinary")) {
            assertSame(bundle.routeIndex().resolveIndex(key, 1).orElseThrow(),
                bundle.routeIndex().resolveRoute(bundle.sectionsByKey().get(key).basePath()).orElseThrow());
        }
    }

    @Test
    void nonOverlappingGeneratedRoutesProduceNoWarnings() throws IOException {
        Path detail = directory("detail");
        write(detail, "foo.md", "# Foo");
        ContentSiteBundle bundle = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("detail", detail, "/articles"))
            .addSection(ContentSectionConfig.create("other", directory("other"), "/foo"))
            .build().generate();
        assertTrue(bundle.warnings().isEmpty());
        assertSame(bundle.routeIndex().resolveDetail("detail", "foo").orElseThrow(),
            bundle.routeIndex().resolveRoute("/articles/foo").orElseThrow());
        assertSame(bundle.routeIndex().resolveIndex("other", 1).orElseThrow(),
            bundle.routeIndex().resolveRoute("/foo").orElseThrow());
    }

    @Test
    void duplicateNormalizedIndexBasesAndSectionKeysStillReject() throws IOException {
        Path directory = directory("content");
        var builder = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("first", directory, "/foo"));
        assertEquals("Duplicate basePath: /foo", assertThrows(IllegalArgumentException.class,
            () -> builder.addSection(ContentSectionConfig.create("second", directory, "foo/"))).getMessage());
        assertEquals("Duplicate sectionKey: first", assertThrows(IllegalArgumentException.class,
            () -> builder.addSection(ContentSectionConfig.create("first", directory, "/other"))).getMessage());
        assertTrue(builder.build().generate().warnings().isEmpty());
    }

    private Path directory(String name) throws IOException {
        return Files.createDirectories(root.resolve(name)).toAbsolutePath().normalize();
    }

    private Path write(Path directory, String name, String content) throws IOException {
        return Files.writeString(directory.resolve(name), content).toAbsolutePath().normalize();
    }

    private void assertCollision(ContentWarning warning, String route, String detailKey, Path file,
                                 String indexKey, Path directory) {
        assertEquals(detailKey, warning.sectionKey());
        assertEquals(file, warning.sourcePath());
        assertTrue(warning.message().contains("Route collision at '" + route + "'"));
        assertTrue(warning.message().contains("detail in section '" + detailKey + "' from file '" + file + "'"));
        assertTrue(warning.message().contains("index in section '" + indexKey + "' from directory '" + directory + "'"));
        assertTrue(warning.message().contains("Detail routes take precedence, including page queries"));
        assertTrue(warning.message().contains("index pagination is masked"));
        assertTrue(warning.message().contains("Direct resolveIndex lookup remains available"));
        assertTrue(warning.message().contains("Choose non-overlapping section base paths or published slugs"));
    }
}
