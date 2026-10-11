package io.mindspice.simplypages.components.content;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.HashSet;
import static org.junit.jupiter.api.Assertions.*;

class ContentEdgeCaseTest {
    @TempDir Path root;
    private ContentSiteBundle generate(String base) {
        return StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("test", root, base)).build().generate();
    }
    @Test void malformedQuoteDoesNotCrashGeneration() throws Exception {
        Files.writeString(root.resolve("entry.md"), "---\ntitle: '\n---\n# Body");
        assertEquals(1, generate("/posts").sectionsByKey().get("test").entries().size());
    }
    @Test void suffixedSlugsAndHeadingsStayUnique() throws Exception {
        for (int i = 0; i < 3; i++) {
            String slug = i == 2 ? "same-2" : "same";
            Files.writeString(root.resolve(i + ".md"), "---\nslug: " + slug + "\n---\n# A\n## A\n## A-2");
        }
        var section = generate("/posts").sectionsByKey().get("test");
        assertEquals(3, section.detailPagesBySlug().size());
        for (var page : section.detailPagesBySlug().values()) {
            var ids = Jsoup.parse(page.render()).select("h1[id], h2[id]").eachAttr("id");
            assertEquals(ids.size(), new HashSet<>(ids).size());
        }
    }
    @Test void rootSectionUsesLocalDetailLinks() throws Exception {
        Files.writeString(root.resolve("entry.md"), "# Entry");
        var bundle = generate("/");
        assertEquals("/entry", bundle.routeIndex().detailRoute("test", "entry"));
        assertEquals("/entry", bundle.sectionsByKey().get("test").entries().getFirst().route());
        assertTrue(bundle.routeIndex().resolveRoute("/entry").isPresent());
    }

    @Test void builtSiteSnapshotsMutableConfiguration() throws Exception {
        Files.writeString(root.resolve("a.md"), "# A");
        Files.writeString(root.resolve("b.md"), "# B");
        var config = ContentSectionConfig.create("test", root, "/posts").withPageSize(1);
        var site = StaticContentSiteBuilder.create().addSection(config).build();
        config.withPageSize(10);
        assertEquals(2, site.generate().sectionsByKey().get("test").totalPages());
    }


    @Test void draftsDoNotChangePublishedRoutes() throws Exception {
        Files.writeString(root.resolve("a-draft.md"), "---\nslug: same\ndraft: true\n---\n# Draft");
        Files.writeString(root.resolve("b-public.md"), "---\nslug: same\n---\n# Public");
        assertTrue(generate("/posts").routeIndex().resolveRoute("/posts/same").isPresent());
    }
}
