package io.mindspice.demo;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Principal;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DocumentationApplicationExamplesTest {
    @TempDir Path output;
    private static final Principal OWNER = () -> "operations-owner";
    private static final Principal OTHER = () -> "another-user";

    @Test void blogServesRealFilesPaginationDraftFilteringAndMatchingFragments() throws Exception {
        Path content = Files.createDirectories(output.resolve("blog-content"));
        Files.writeString(content.resolve("first.md"), "---\ntitle: First post\nslug: first-post\ndate: 2026-01-02\n---\n## Details\nFirst article body.");
        Files.writeString(content.resolve("earlier.md"), "---\ntitle: Earlier note\nslug: earlier-note\ndate: 2026-01-01\n---\nEarlier article body.");
        Files.writeString(content.resolve("draft.md"), "---\ntitle: Unpublished draft\nslug: unpublished\ndraft: true\n---\nPrivate draft text.");
        try (var compiled = compile("build-a-markdown-blog")) {
            Object controller = compiled.type("MarkdownBlogController").getConstructor(Path.class).newInstance(content);
            MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();
            var full = mvc.perform(get("/guide/blog")).andExpect(status().isOk()).andReturn().getResponse();
            String first = html(full);
            assertTrue(first.toLowerCase().contains("<!doctype"));
            assertTrue(first.contains("First post"));
            assertFalse(first.contains("Private draft text"));
            assertEquals("HX-Request", full.getHeader("Vary"));
            String second = html(mvc.perform(get("/guide/blog").param("page", "2").header("HX-Request", "true"))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertTrue(second.contains("Earlier note"));
            assertFalse(second.toLowerCase().contains("<!doctype"));
            assertEquals(1, Jsoup.parse(second).select("#blog-view").size());
            String article = html(mvc.perform(get("/guide/blog/first-post").header("HX-Request", "true"))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertTrue(article.contains("First article body"));
            assertFalse(article.contains("Earlier article body"));
            mvc.perform(get("/guide/blog/unpublished")).andExpect(status().isNotFound());
            mvc.perform(get("/guide/blog/missing")).andExpect(status().isNotFound());
            mvc.perform(get("/guide/blog").param("page", "3")).andExpect(status().isNotFound());
        }
    }

    @Test void forumScopesAndPaginatesActualTopicAndCommentHandlers() throws Exception {
        try (var compiled = compile("build-a-forum")) {
            MockMvc mvc = mvc(compiled, "ReadOnlyForumController");
            String first = html(mvc.perform(get("/guide/forum")).andExpect(status().isOk()).andReturn().getResponse());
            assertTrue(first.contains("Welcome"));
            assertTrue(first.contains("Writing posts"));
            String second = html(mvc.perform(get("/guide/forum").param("page", "2").header("HX-Request", "true"))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertTrue(second.contains("Useful links"));
            assertEquals(1, Jsoup.parse(second).select("#forum-view").size());
            assertFalse(second.toLowerCase().contains("<!doctype"));
            String thread = html(mvc.perform(get("/guide/forum/topics/welcome").header("HX-Request", "true"))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertTrue(thread.contains("I am building a reading list"));
            assertTrue(thread.contains("A shared list sounds useful"));
            assertFalse(thread.contains("Code fences help too"));
            String next = html(mvc.perform(get("/guide/forum/topics/welcome").param("page", "2").header("HX-Request", "true"))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertTrue(next.contains("I am documenting our tools"));
            assertFalse(next.contains("A shared list sounds useful"));
            mvc.perform(get("/guide/forum/topics/missing")).andExpect(status().isNotFound());
            mvc.perform(get("/guide/forum").param("page", "0")).andExpect(status().isBadRequest());
        }
    }

    @Test void chatActuallyPostsPollsScopesSessionsValidatesTokensAndBoundsHistory() throws Exception {
        try (var compiled = compile("add-chat")) {
            MockMvc mvc = mvc(compiled, "SessionChatController");
            MockHttpSession session = new MockHttpSession();
            String page = html(mvc.perform(get("/guide/chat").session(session)).andExpect(status().isOk()).andReturn().getResponse());
            String token = Jsoup.parse(page).select("input[name=csrf]").val();
            assertFalse(token.isBlank());
            assertEquals("every 2s", Jsoup.parse(page).selectFirst("#guide-chat-history").attr("hx-trigger"));
            String sent = html(mvc.perform(post("/guide/chat/messages").session(session).header("HX-Request", "true")
                .param("csrf", token).param("message", "Hello **there**"))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertTrue(Jsoup.parse(sent).text().contains("Hello there"));
            assertEquals(1, Jsoup.parse(sent).select("#guide-chat-history").size());
            assertEquals("every 2s", Jsoup.parse(sent).selectFirst("#guide-chat-history").attr("hx-trigger"));
            String foreign = html(mvc.perform(get("/guide/chat/history").session(new MockHttpSession()))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertFalse(foreign.contains("Hello"));
            mvc.perform(post("/guide/chat/messages").session(session).param("csrf", "wrong").param("message", "forbidden"))
                .andExpect(status().isForbidden());
            mvc.perform(post("/guide/chat/messages").session(session).param("csrf", token).param("message", " "))
                .andExpect(status().isBadRequest());
            mvc.perform(post("/guide/chat/messages").session(session).param("csrf", token).param("message", "x".repeat(2001)))
                .andExpect(status().isBadRequest());
            for (int index = 0; index < 101; index++) {
                mvc.perform(post("/guide/chat/messages").session(session).header("HX-Request", "true")
                    .param("csrf", token).param("message", "Message " + index)).andExpect(status().isOk());
            }
            var history = Jsoup.parse(html(mvc.perform(get("/guide/chat/history").session(session))
                .andExpect(status().isOk()).andReturn().getResponse()));
            assertEquals(100, history.select(".chat-message").size());
            assertFalse(history.select(".chat-message-body").eachText().contains("Message 0"));
            assertTrue(history.select(".chat-message-body").eachText().contains("Message 100"));
            assertFalse(history.text().contains("forbidden"));
        }
    }

    @Test void editablePageEnforcesOwnerAndKeepsInvalidDraftsOutOfStoredState() throws Exception {
        try (var compiled = compile("build-user-editable-pages")) {
            MockMvc mvc = mvc(compiled, "EditableBriefingController");
            mvc.perform(get("/learn/editable-page")).andExpect(status().isUnauthorized());
            mvc.perform(get("/learn/editable-page/edit").principal(OTHER)).andExpect(status().isForbidden());
            mvc.perform(post("/learn/editable-page/save").principal(OTHER)
                .param("title", "Unauthorized").param("content", "Unauthorized")).andExpect(status().isForbidden());
            String edit = html(mvc.perform(get("/learn/editable-page/edit").principal(OWNER))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertEquals(1, Jsoup.parse(edit).select("[role=dialog]").size());
            String invalid = html(mvc.perform(post("/learn/editable-page/save").principal(OWNER)
                .param("title", "Draft <title>").param("content", ""))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertEquals("Draft <title>", Jsoup.parse(invalid).select("input[name=title]").val());
            String unchanged = html(mvc.perform(get("/learn/editable-page").principal(OWNER))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertTrue(unchanged.contains("Shift briefing"));
            String saved = html(mvc.perform(post("/learn/editable-page/save").principal(OWNER)
                .param("title", "Updated briefing").param("content", "<script>not executable</script>"))
                .andExpect(status().isOk()).andReturn().getResponse());
            var result = Jsoup.parse(saved);
            assertEquals(1, result.select("#briefing-page").size());
            assertTrue(result.select("#briefing-page").text().contains("<script>not executable</script>"));
            assertTrue(result.select("script").isEmpty());
            assertEquals(1, result.select("#briefing-modal-host").size());
            assertTrue(result.selectFirst("#briefing-modal-host").children().isEmpty());
            String reloaded = html(mvc.perform(get("/learn/editable-page").principal(OWNER))
                .andExpect(status().isOk()).andReturn().getResponse());
            assertTrue(reloaded.contains("Updated briefing"));
        }
    }

    @Test void formSupportsOrdinaryAndHtmxValidationPreservesValuesAndEscapesConfirmation() throws Exception {
        try (var compiled = compile("handle-form-submissions")) {
            MockMvc mvc = mvc(compiled, "ContactPreviewController");
            mvc.perform(get("/learn/contact-preview").requestAttr("appCsrfToken", "issued-token"))
                .andExpect(status().isUnauthorized());
            String page = html(mvc.perform(get("/learn/contact-preview").principal(OWNER)
                .requestAttr("appCsrfToken", "issued-token")).andExpect(status().isOk()).andReturn().getResponse());
            assertEquals("issued-token", Jsoup.parse(page).select("input[name=_csrf]").val());
            var failed = mvc.perform(post("/learn/contact-preview").principal(OWNER).requestAttr("appCsrfToken", "issued-token")
                .header("HX-Request", "true").param("name", "<draft>").param("email", "invalid"))
                .andExpect(status().isOk()).andReturn().getResponse();
            var fields = Jsoup.parse(html(failed));
            assertEquals("<draft>", fields.select("#contact-name").val());
            assertEquals("invalid", fields.select("#contact-email").val());
            assertEquals("true", fields.selectFirst("#contact-email").attr("aria-invalid"));
            assertEquals(1, fields.select("#contact-result").size());
            assertEquals("HX-Request", failed.getHeader("Vary"));
            var valid = mvc.perform(post("/learn/contact-preview").principal(OWNER).requestAttr("appCsrfToken", "issued-token")
                .header("HX-Request", "true").param("name", "<b>Ada</b>").param("email", "ada@example.org"))
                .andExpect(status().isOk()).andReturn().getResponse();
            var confirmed = Jsoup.parse(html(valid));
            assertTrue(confirmed.text().contains("Name: <b>Ada</b>"));
            assertTrue(confirmed.select("b").isEmpty());
            assertTrue(confirmed.text().contains("Nothing was sent or stored"));
            var ordinary = mvc.perform(post("/learn/contact-preview").principal(OWNER).requestAttr("appCsrfToken", "issued-token")
                .param("name", " ").param("email", "invalid"))
                .andExpect(status().isBadRequest()).andReturn().getResponse();
            assertTrue(html(ordinary).toLowerCase().contains("<!doctype"));
        }
    }

    @Test void dashboardDerivesMetricsAndStableRowsFromOneSnapshot() throws Exception {
        try (var compiled = compile("build-a-dashboard")) {
            MockMvc mvc = mvc(compiled, "DashboardController");
            for (int request = 0; request < 2; request++) {
                String body = html(mvc.perform(get("/guide/dashboard")).andExpect(status().isOk()).andReturn().getResponse());
                var page = Jsoup.parse(body);
                assertTrue(body.toLowerCase().contains("<!doctype"));
                assertEquals(1, page.select("#dashboard-stats").size());
                assertEquals(3, page.select("table tbody tr").size());
                assertEquals(java.util.List.of("Access 7 2 Alex", "Deploys 4 0 Sam", "Billing 9 1 Jo"),
                    page.select("table tbody tr").eachText());
                assertTrue(page.text().contains("3 overdue items across 2 queues need review"));
                assertTrue(page.select("#dashboard-stats").text().contains("20"));
                assertEquals(1, page.select(".col-8").size());
                assertEquals(1, page.select(".col-4").size());
            }
        }
    }

    private DocumentationExamplesTest.Compilation compile(String task) throws Exception {
        return DocumentationExamplesTest.compile("how-to/" + task + ".md", output.resolve(task));
    }

    private static MockMvc mvc(DocumentationExamplesTest.Compilation compilation, String name) throws Exception {
        return MockMvcBuilders.standaloneSetup(compilation.type(name).getConstructor().newInstance()).build();
    }

    private static String html(org.springframework.mock.web.MockHttpServletResponse response) throws Exception {
        return response.getContentAsString(StandardCharsets.UTF_8);
    }
}
