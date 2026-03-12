package io.mindspice.simplypages.components.content;

import io.mindspice.simplypages.core.Component;
import io.mindspice.simplypages.testutil.HtmlAssert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticContentSiteTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("StaticContentSite should generate query-paginated index pages with default styled metadata")
    void generatesPaginatedIndexPagesWithStyledMetadata() throws IOException {
        Path blogs = tempDir.resolve("blogs");
        write(blogs.resolve("alpha.md"), """
            ---
            title: Alpha Post
            summary: Alpha summary
            date: 2026-03-10
            author: Alpha Team
            tags: java,simplypages
            ---
            # Alpha
            Alpha content.
            """);

        write(blogs.resolve("bravo.md"), """
            ---
            title: Bravo Post
            summary: Bravo summary
            date: 2026-03-11T14:30
            author: Bravo Team
            tags: docs,helper
            slug: bravo-post
            ---
            # Bravo
            Bravo content.
            """);

        write(blogs.resolve("charlie.md"), """
            ---
            title: Charlie Post
            date: 2026-03-09
            ---
            # Charlie
            Charlie content.
            """);

        write(blogs.resolve("draft.md"), """
            ---
            title: Hidden Draft
            date: 2026-03-12T10:00
            draft: true
            ---
            # Hidden
            Draft content.
            """);

        StaticContentSite site = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("blogs", blogs, "/blogs").withPageSize(2))
            .build();

        ContentSiteBundle bundle = site.generate();
        ContentRouteIndex routes = bundle.routeIndex();

        assertTrue(routes.resolveRoute("/blogs").isPresent());
        assertTrue(routes.resolveRoute("/blogs?page=2").isPresent());
        assertTrue(routes.resolveRequest("/blogs", "2").isPresent());
        assertTrue(routes.resolveDetail("blogs", "bravo-post").isPresent());

        String page1Html = routes.resolveRoute("/blogs").orElseThrow().render();
        HtmlAssert.assertThat(page1Html)
            .hasElementCount(".sp-content-list-item", 2)
            .hasElementCount(".sp-content-list-byline", 2)
            .hasElement(".sp-content-list-tags .tag")
            .attributeEquals(".sp-content-page-next", "href", "/blogs?page=2");

        Document page1Doc = Jsoup.parse(page1Html);
        assertEquals(List.of("Bravo Post", "Alpha Post"), page1Doc.select(".sp-content-list-title a").eachText());
        assertEquals(List.of("By Bravo Team on 2026-03-11 14:30", "By Alpha Team on 2026-03-10 00:00"),
            page1Doc.select(".sp-content-list-byline").eachText());
        assertFalse(page1Doc.text().contains("Hidden Draft"));

        String page2Html = routes.resolveRoute("/blogs?page=2").orElseThrow().render();
        HtmlAssert.assertThat(page2Html)
            .hasElementCount(".sp-content-list-item", 1)
            .elementTextEquals(".sp-content-list-title a", "Charlie Post")
            .attributeEquals(".sp-content-page-prev", "href", "/blogs");
    }

    @Test
    @DisplayName("StaticContentSite should render detail pages with sticky TOC from h2-h4 headings")
    void rendersDetailPagesWithStickyToc() throws IOException {
        Path projects = tempDir.resolve("projects");
        write(projects.resolve("parser.md"), """
            ---
            title: Parser Project
            date: 2026-03-11T09:45
            author: Parser Team
            tags: parser,java
            ---
            # Parser Project
            Intro paragraph.

            ## Architecture
            Overview.

            ### Tokenization Layer
            Details.

            #### Long Lower Heading Name That Should Truncate In Sidebar Styling
            Extra.

            ##### Deep Heading Not In Toc
            Deep details.
            """);

        StaticContentSite site = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("projects", projects, "/projects"))
            .build();

        ContentSiteBundle bundle = site.generate();
        Component detail = bundle.routeIndex().resolveRoute("/projects/parser").orElseThrow();
        String html = detail.render();

        HtmlAssert.assertThat(html)
            .hasElement(".page-content.with-sticky-sidebar")
            .hasElementCount(".sp-content-toc-item", 3)
            .hasElement(".sp-content-detail-byline")
            .hasElement(".sp-content-toc-item.level-3")
            .hasElement(".sp-content-toc-item.level-4")
            .doesNotHaveElement(".sp-content-toc-item.level-5")
            .hasElement(".sp-content-markdown-body h2[id]")
            .hasElement(".sp-content-markdown-body h3[id]")
            .hasElement(".sp-content-markdown-body h4[id]");

        Document doc = Jsoup.parse(html);
        for (Element item : doc.select(".sp-content-toc-item")) {
            String href = item.attr("href");
            assertTrue(href.startsWith("#"));
            assertNotNull(doc.selectFirst(href));
        }
    }

    @Test
    @DisplayName("StaticContentSite should keep invalid frontmatter files as best-effort content and return warnings")
    void bestEffortInvalidFrontmatter() throws IOException {
        Path blogs = tempDir.resolve("blogs");
        write(blogs.resolve("bad-meta.md"), """
            ---
            date: not-a-date
            tags: one, two, one
            draft: maybe
            malformedline
            ---
            First paragraph with fallback summary.
            """);

        StaticContentSite site = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("blogs", blogs, "/blogs"))
            .build();

        ContentSiteBundle bundle = site.generate();
        ContentSectionSite section = bundle.sectionsByKey().get("blogs");

        assertEquals(1, section.entries().size());
        ContentEntryRecord entry = section.entries().getFirst();
        assertEquals("bad-meta", entry.slug());
        assertEquals("Bad Meta", entry.title());
        assertNull(entry.publishedAt());
        assertEquals(List.of("one", "two"), entry.tags());
        assertTrue(bundle.warnings().size() >= 2);

        String indexHtml = bundle.routeIndex().resolveRoute("/blogs").orElseThrow().render();
        HtmlAssert.assertThat(indexHtml)
            .doesNotHaveElement(".sp-content-list-byline")
            .hasElementCount(".sp-content-list-tags .tag", 2);
    }

    @Test
    @DisplayName("StaticContentSite should respect section maxDepth while scanning markdown files")
    void respectsMaxDepthDuringDiscovery() throws IOException {
        Path blogs = tempDir.resolve("blogs");
        write(blogs.resolve("top.md"), """
            ---
            title: Top
            date: 2026-03-11
            ---
            Top content.
            """);

        write(blogs.resolve("deep/inside.md"), """
            ---
            title: Deep
            date: 2026-03-10
            ---
            Deep content.
            """);

        StaticContentSite site = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("blogs", blogs, "/blogs").withMaxDepth(1))
            .build();

        ContentSiteBundle bundle = site.generate();

        assertEquals(1, bundle.sectionsByKey().get("blogs").entries().size());
        assertTrue(bundle.routeIndex().resolveRoute("/blogs/top").isPresent());
        assertFalse(bundle.routeIndex().resolveRoute("/blogs/deep-inside").isPresent());
    }

    @Test
    @DisplayName("StaticContentSite should escape raw HTML by default in markdown detail rendering")
    void escapesRawHtmlByDefault() throws IOException {
        Path blogs = tempDir.resolve("blogs");
        write(blogs.resolve("unsafe.md"), """
            ---
            title: Unsafe Content
            date: 2026-03-11
            ---
            <script>alert('x')</script>

            ## Heading
            body
            """);

        StaticContentSite site = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create("blogs", blogs, "/blogs"))
            .build();

        String detailHtml = site.generate()
            .routeIndex()
            .resolveRoute("/blogs/unsafe")
            .orElseThrow()
            .render();

        HtmlAssert.assertThat(detailHtml)
            .doesNotHaveElement("script")
            .hasElement(".sp-content-markdown-body h2[id]");
        assertTrue(detailHtml.contains("&lt;script&gt;alert('x')&lt;/script&gt;"));
    }

    private void write(Path filePath, String content) throws IOException {
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
    }
}
