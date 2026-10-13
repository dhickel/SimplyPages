package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import io.mindspice.demo.EditingDemoController;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class DemoHtmlHttpIntegrationTest {

    private static final String UNRELATED_VARY_TOKEN = "Accept-Encoding";
    private static final String HX_VARY_TOKEN = "HX-Request";

    private static final List<HxCase> HX_CASES = Arrays.asList(
        new HxCase("missing", null, false),
        new HxCase("false", "false", false),
        new HxCase("empty", "", false),
        new HxCase("junk", "junk", false),
        new HxCase("true", "true", true),
        new HxCase("mixed case true", "TrUe", true),
        new HxCase("whitespace true", " true ", false)
    );

    @org.springframework.beans.factory.annotation.Autowired
    private WebApplicationContext context;
    private MockMvc mockMvc;

    @BeforeEach
    void isolatedVarySeed() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(new SeedVaryFilter()).build();
    }

    @Test
    @DisplayName("All document mappings use HTML and select fragments only for exact true HX requests")
    void documentHtmlAndHxMatrix() throws Exception {
        List<DocumentRoute> routes = List.of(
            new DocumentRoute("root", () -> get("/")),
            new DocumentRoute("home", () -> get("/home")),
            new DocumentRoute("demos", () -> get("/demos")),
            new DocumentRoute("basics", () -> get("/demos/basics-forms")),
            new DocumentRoute("display", () -> get("/demos/display-data")),
            new DocumentRoute("modules", () -> get("/demos/modules")),
            new DocumentRoute("htmx editing", () -> get("/demos/htmx-editing")),
            new DocumentRoute("javadocs", () -> get("/javadocs-view")),
            new DocumentRoute("docs home", () -> get("/docs")),
            new DocumentRoute("docs detail", () -> get("/docs/core/03-template-rendercontext-slotkey-reference.md")),
            new DocumentRoute("editing demo", () -> get("/editing-demo")),
            new DocumentRoute("blog", () -> get("/blog")),
            new DocumentRoute("blog detail", () -> get("/blog/template-slotkey-render-context")),
            new DocumentRoute("forum", () -> get("/forum"))
        );

        for (DocumentRoute route : routes) {
            for (HxCase hx : HX_CASES) {
                MockHttpServletRequestBuilder request = route.request().get();
                if (hx.value() != null) {
                    request.header(HX_VARY_TOKEN, hx.value());
                }

                MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
                assertEquals(200, response.getStatus(), route.name() + " / HX " + hx.name());
                assertHtml(response, route.name() + " / HX " + hx.name());
                assertVary(response, route.name() + " / HX " + hx.name());

                String body = response.getContentAsString();
                if (hx.selectsFragment()) {
                    assertFalse(body.contains("<!DOCTYPE html>"), route.name() + " should be a fragment for exact true");
                } else {
                    assertTrue(body.contains("<!DOCTYPE html>"), route.name() + " should remain a document for HX " + hx.name());
                }
            }
        }
    }

    @Test
    @DisplayName("Fragment, modal, and forum mappings remain HTML for every HX spelling")
    void fragmentHtmlAndHxMatrix() throws Exception {
        List<FragmentRoute> routes = List.of(
            new FragmentRoute("editing modal", () -> get("/editing-demo/edit/module-1")),
            new FragmentRoute("pending edits modal", () -> get("/editing-demo/pending-edits")),
            new FragmentRoute("add module modal", () -> get("/editing-demo/add-module-modal/row-1")),
            new FragmentRoute("forum topics", () -> get("/forum/topics")
                .param("scope", "cat-feedback")
                .param("view", "topics")
                .param("topicPage", "1")
                .param("topicSize", "8")
                .param("commentPage", "1")
                .param("commentSize", "8")),
            new FragmentRoute("forum comments", () -> get("/forum/topics/missing-topic/comments")),
            new FragmentRoute("chat history", () -> get("/chat/history").param("conversationId", "matrix-conversation"))
        );

        for (FragmentRoute route : routes) {
            for (HxCase hx : HX_CASES) {
                MockHttpServletRequestBuilder request = route.request().get();
                if (hx.value() != null) {
                    request.header(HX_VARY_TOKEN, hx.value());
                }

                MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
                assertEquals(200, response.getStatus(), route.name() + " / HX " + hx.name());
                assertHtml(response, route.name() + " / HX " + hx.name());
                assertVary(response, route.name() + " / HX " + hx.name());
                assertFalse(response.getContentAsString().contains("<!DOCTYPE html>"),
                    route.name() + " must remain a fragment / HX " + hx.name());
            }
        }
    }

    @Test
    @DisplayName("Chat message responses remain truthful HTML for every HX spelling")
    void chatMessageHtmlMatrix() throws Exception {
        for (HxCase hx : HX_CASES) {
            MockHttpServletRequestBuilder request = post("/chat/messages")
                .param("conversationId", "matrix-message")
                .param("message", "");
            if (hx.value() != null) {
                request.header(HX_VARY_TOKEN, hx.value());
            }

            MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
            assertEquals(400, response.getStatus(), "chat message / HX " + hx.name());
            assertHtml(response, "chat message / HX " + hx.name());
            assertVary(response, "chat message / HX " + hx.name());
        }
    }

    @Test
    @DisplayName("Forum rendered mutations and failures remain HTML for every HX spelling")
    void forumMutationHtmlMatrix() throws Exception {
        List<StatusRoute> routes = List.of(
            new StatusRoute("viewer", () -> post("/forum/viewer").param("displayName", "Matrix User"), 200),
            new StatusRoute("topic create validation", () -> post("/forum/topics/create"), 400),
            new StatusRoute("comment create validation", () -> post("/forum/comments/create"), 400),
            new StatusRoute("missing topic quote", () -> post("/forum/topics/missing-topic/quote"), 404),
            new StatusRoute("missing comment quote", () -> post("/forum/comments/missing-comment/quote"), 404),
            new StatusRoute("missing topic edit", () -> post("/forum/topics/missing-topic/edit"), 404),
            new StatusRoute("missing comment edit", () -> post("/forum/comments/missing-comment/edit"), 404),
            new StatusRoute("missing topic update", () -> post("/forum/topics/missing-topic/update"), 404),
            new StatusRoute("missing comment update", () -> post("/forum/comments/missing-comment/update"), 404),
            new StatusRoute("missing topic delete", () -> post("/forum/topics/missing-topic/delete"), 404),
            new StatusRoute("missing comment delete", () -> post("/forum/comments/missing-comment/delete"), 404)
        );

        for (StatusRoute route : routes) {
            for (HxCase hx : HX_CASES) {
                MockHttpServletRequestBuilder request = route.request().get();
                if (hx.value() != null) {
                    request.header(HX_VARY_TOKEN, hx.value());
                }
                MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
                assertEquals(route.status(), response.getStatus(), route.name() + " / HX " + hx.name());
                assertHtml(response, route.name() + " / HX " + hx.name());
                assertVary(response, route.name() + " / HX " + hx.name());
            }
        }
    }

    @Test
    @DisplayName("Chat preserves exact-true redirect behavior and event-stream media")
    void chatRedirectAndSseContractsRemainIntact() throws Exception {
        for (HxCase hx : HX_CASES) {
            MockHttpServletRequestBuilder request = get("/chat");
            if (hx.value() != null) {
                request.header(HX_VARY_TOKEN, hx.value());
            }

            MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
            assertEquals(200, response.getStatus(), "chat / HX " + hx.name());
            assertHtml(response, "chat / HX " + hx.name());
            assertVary(response, "chat / HX " + hx.name());
            if (hx.selectsFragment()) {
                assertEquals("/chat", response.getHeader("HX-Redirect"));
                assertEquals("", response.getContentAsString());
            } else {
                assertNull(response.getHeader("HX-Redirect"));
                assertTrue(response.getContentAsString().contains("data-sp-chat=\"true\""));
            }
        }

        mockMvc.perform(get("/chat/stream").param("conversationId", "matrix-sse"))
            .andExpect(result -> assertTrue(result.getRequest().isAsyncStarted()))
            .andExpect(result -> assertEquals(MediaType.TEXT_EVENT_STREAM,
                MediaType.parseMediaType(result.getResponse().getContentType())));
    }

    @Test
    @DisplayName("Template and form preview responses are explicitly HTML")
    void htmlApiMappingsExposeHtml() throws Exception {
        MockHttpServletResponse template = mockMvc.perform(post("/demos/api/template-card")
                .param("title", "Matrix card")
                .param("body", "Matrix body"))
            .andReturn().getResponse();
        assertEquals(200, template.getStatus());
        assertHtml(template, "template card");
        assertTrue(template.getContentAsString().contains("dynamic-card-preview"));

        MockHttpServletResponse form = mockMvc.perform(post("/demos/api/form-preview")
                .param("name", "Matrix user")
                .param("email", "matrix@example.test")
                .param("role", "tester"))
            .andReturn().getResponse();
        assertEquals(200, form.getStatus());
        assertHtml(form, "form preview");
        assertTrue(form.getContentAsString().contains("Matrix user"));
    }

    @Test
    @DisplayName("HTML mappings negotiate default, HTML, and wildcard accepts, rejecting JSON")
    void htmlAcceptMatrix() throws Exception {
        List<Supplier<MockHttpServletRequestBuilder>> documents = List.of(
            () -> get("/home"),
            () -> get("/docs"),
            () -> get("/editing-demo"),
            () -> get("/blog"),
            () -> get("/forum"),
            () -> get("/chat"),
            () -> post("/demos/api/template-card").param("title", "Accept card").param("body", "Accept body"),
            () -> post("/demos/api/form-preview").param("name", "Accept user")
        );

        for (Supplier<MockHttpServletRequestBuilder> document : documents) {
            for (String accept : Arrays.asList(null, MediaType.TEXT_HTML_VALUE, MediaType.ALL_VALUE)) {
                MockHttpServletRequestBuilder request = document.get();
                if (accept != null) {
                    request.header("Accept", accept);
                }
                MockHttpServletResponse response = mockMvc.perform(request).andReturn().getResponse();
                assertEquals(200, response.getStatus(), "accepted " + accept);
                assertHtml(response, "accepted " + accept);
            }

            MockHttpServletResponse incompatible = mockMvc.perform(document.get()
                    .header("Accept", MediaType.APPLICATION_JSON_VALUE))
                .andReturn().getResponse();
            assertEquals(406, incompatible.getStatus(), "incompatible Accept for HTML route");
        }

        MockHttpServletResponse fragment = mockMvc.perform(get("/chat/history")
                .param("conversationId", "accept-matrix")
                .header("Accept", MediaType.TEXT_HTML_VALUE))
            .andReturn().getResponse();
        assertEquals(200, fragment.getStatus());
        assertHtml(fragment, "HTML fragment accept");

        MockHttpServletResponse incompatibleFragment = mockMvc.perform(get("/chat/history")
                .param("conversationId", "accept-matrix")
                .header("Accept", MediaType.APPLICATION_JSON_VALUE))
            .andReturn().getResponse();
        assertEquals(406, incompatibleFragment.getStatus());
    }

    @Test
    @DisplayName("Rendered OOB editing responses are HTML and vary on HX without changing HX truthiness")
    void editingOobResponseHtmlMatrix() throws Exception {
        EditingDemoController controller = new EditingDemoController();
        MockMvc isolated = isolatedEditingMvc(controller);

        for (HxCase hx : HX_CASES) {
            MockHttpServletRequestBuilder request = post("/editing-demo/save/module-1")
                .param("title", "Matrix title")
                .param("content", "Matrix content")
                .param("width", "6")
                .param("useMarkdown", "true");
            if (hx.value() != null) {
                request.header(HX_VARY_TOKEN, hx.value());
            }

            MockHttpServletResponse response = isolated.perform(request).andReturn().getResponse();
            assertEquals(200, response.getStatus(), "save / HX " + hx.name());
            assertHtml(response, "save / HX " + hx.name());
            assertVary(response, "save / HX " + hx.name());
            assertTrue(response.getContentAsString().contains("hx-swap-oob=\"true\""));
        }
    }

    @Test
    void everyEditingRenderedMappingHasHtmlAndPreservesVary() throws Exception {
        List<Supplier<MockHttpServletRequestBuilder>> routes = List.of(
            () -> get("/editing-demo/edit/module-9"),
            () -> get("/editing-demo/edit/missing"),
            () -> post("/editing-demo/save/module-1").param("content", "Changed"),
            () -> post("/editing-demo/save/missing"),
            () -> delete("/editing-demo/delete/module-1"),
            () -> post("/editing-demo/insert-row/0"),
            () -> get("/editing-demo/add-module-modal/row-5"),
            () -> post("/editing-demo/add-module/row-5").param("title", "Added").param("content", "Added"),
            () -> get("/editing-demo/pending-edits"),
            () -> post("/editing-demo/pending-edits/module-2/approve"),
            () -> delete("/editing-demo/pending-edits/module-2/reject"),
            () -> get("/editing-demo/edit-child/module-9/item-0"),
            () -> post("/editing-demo/save-child/module-9/item-0").param("text", "Changed child"),
            () -> delete("/editing-demo/delete-child/module-9/item-0")
        );
        for (var route : routes) for (HxCase hx : HX_CASES) {
            MockMvc isolated = isolatedEditingMvc(new EditingDemoController());
            isolated.perform(post("/editing-demo/save/module-2").param("editMode", "USER_EDIT").param("content", "Pending"));
            var request = route.get();
            if (hx.value() != null) request.header("HX-Request", hx.value());
            var response = isolated.perform(request).andReturn().getResponse();
            assertEquals(200, response.getStatus());
            assertHtml(response, "editing rendered mapping");
            assertVary(response, "editing rendered mapping");
            assertFalse(response.getContentAsString().contains("<!DOCTYPE html>"));
        }
        for (HxCase hx : HX_CASES) for (var request : List.of(
            post("/demos/api/template-card").param("title", "<tag>").param("body", "Body"),
            post("/demos/api/form-preview").param("name", "<script>"))) {
            if (hx.value() != null) request.header("HX-Request", hx.value());
            var response = mockMvc.perform(request).andReturn().getResponse();
            assertEquals(200, response.getStatus());
            assertHtml(response, "fragment API");
            assertFalse(response.getContentAsString().contains("<!DOCTYPE html>"));
            assertFalse(response.getContentAsString().contains("<script>"));
        }
    }

    @Test
    void childErrorsRemainHtmlUnderCompatibleAcceptAndDoNotBlanket406() throws Exception {
        for (var route : List.of(
                new StatusRoute("malformed child", () -> get("/editing-demo/edit-child/module-9/item-01"), 400),
                new StatusRoute("missing child", () -> get("/editing-demo/edit-child/module-9/item-3"), 404),
                new StatusRoute("blank child", () -> post("/editing-demo/save-child/module-9/item-0").param("text", ""), 400))) {
            for (String accept : Arrays.asList(null, "text/html", "*/*")) {
                MockMvc isolated = isolatedEditingMvc(new EditingDemoController());
                var request = route.request().get();
                if (accept != null) request.header("Accept", accept);
                var response = isolated.perform(request).andReturn().getResponse();
                assertEquals(route.status(), response.getStatus());
                assertHtml(response, route.name());
                assertVary(response, route.name());
                assertEquals("true", response.getHeader("X-Editing-Demo-Modal"));
            }
            var incompatible = isolatedEditingMvc(new EditingDemoController())
                .perform(route.request().get().header("Accept", "application/json")).andReturn().getResponse();
            assertEquals(406, incompatible.getStatus());
            assertNull(incompatible.getHeader("X-Editing-Demo-Modal"));
        }
    }

    @Test
    @DisplayName("Documentation and blog misses are 404 HTML with escaped request paths")
    void missingContentIsEscapedHtml404() throws Exception {
        String maliciousPath = "missing/&lt;script&gt;";
        String escapedPath = "missing/&amp;lt;script&amp;gt;";

        MockHttpServletResponse docs = mockMvc.perform(get("/docs/" + maliciousPath))
            .andReturn().getResponse();
        assertEquals(404, docs.getStatus());
        assertHtml(docs, "missing docs");
        assertTrue(docs.getContentAsString().contains(escapedPath));
        assertFalse(docs.getContentAsString().contains(maliciousPath));
        assertVary(docs, "missing docs");

        MockHttpServletResponse blog = mockMvc.perform(get("/blog/" + maliciousPath))
            .andReturn().getResponse();
        assertEquals(404, blog.getStatus());
        assertHtml(blog, "missing blog");
        assertTrue(blog.getContentAsString().contains(escapedPath));
        assertFalse(blog.getContentAsString().contains(maliciousPath));
        assertVary(blog, "missing blog");
    }

    @Test
    @DisplayName("Editing insertion accepts only -1 through the current last row and preserves state on invalid input")
    void editingInsertionRangeAndInvalidStateMatrix() throws Exception {
        for (String validPosition : List.of("-1", "0", "4")) {
            EditingDemoController controller = new EditingDemoController();
            MockMvc isolated = isolatedEditingMvc(controller);
            int originalCount = rowCount(controller);
            String before = editingStateFingerprint(controller);
            int beforeCounter = counter(controller);

            MockHttpServletResponse response = isolated.perform(post("/editing-demo/insert-row/{position}", validPosition))
                .andReturn().getResponse();
            assertEquals(200, response.getStatus(), "valid insertion " + validPosition);
            assertHtml(response, "valid insertion " + validPosition);
            assertEquals(originalCount + 1, rowCount(controller));
            assertEquals(beforeCounter + 1, counter(controller));
            assertNotEquals(before, editingStateFingerprint(controller));
            assertTrue(response.getContentAsString().contains("row-101"));
            assertVary(response, "valid insertion " + validPosition);
            assertEquals("row-101", rowId(controller, Integer.parseInt(validPosition) + 1));
            Object page = pages(controller).get("demo-page");
            List<?> rows = (List<?>) readField(page, "rows");
            for (int i = 0; i < rows.size(); i++) assertEquals(i, readField(rows.get(i), "position"));
        }

        EditingDemoController emptyController = new EditingDemoController();
        clearRows(emptyController);
        MockMvc emptyMvc = isolatedEditingMvc(emptyController);
        for (String invalid : List.of("0", "-2", "2147483647", "2147483648", "junk")) {
            String before = editingStateFingerprint(emptyController);
            int beforeCounter = counter(emptyController);
            MockHttpServletResponse error = emptyMvc.perform(post("/editing-demo/insert-row/{position}", invalid)).andReturn().getResponse();
            assertEquals(400, error.getStatus());
            assertHtml(error, "invalid empty insertion");
            assertVary(error, "invalid empty insertion");
            assertEquals(before, editingStateFingerprint(emptyController));
            assertEquals(beforeCounter, counter(emptyController));
        }
        MockHttpServletResponse empty = emptyMvc.perform(post("/editing-demo/insert-row/-1"))
            .andReturn().getResponse();
        assertEquals(200, empty.getStatus());
        assertHtml(empty, "empty page insertion");
        assertEquals(1, rowCount(emptyController));
        assertEquals("row-101", rowId(emptyController, 0));

        for (String invalidPosition : List.of(
            "-2", "5", String.valueOf(Integer.MIN_VALUE), String.valueOf(Integer.MAX_VALUE),
            "not-a-number", "2147483648", "-2147483649", "0x80000000", "-0x80000001", "0x5", "- 2", "0xjunk"
        )) {
            EditingDemoController controller = new EditingDemoController();
            MockMvc isolated = isolatedEditingMvc(controller);
            isolated.perform(post("/editing-demo/save/module-2").param("editMode", "USER_EDIT").param("content", "Retained proposal"));
            String before = editingStateFingerprint(controller);
            int beforeCounter = counter(controller);

            MockHttpServletResponse response = isolated.perform(post("/editing-demo/insert-row/{position}", invalidPosition))
                .andReturn().getResponse();
            assertEquals(400, response.getStatus(), "invalid insertion " + invalidPosition);
            assertHtml(response, "invalid insertion " + invalidPosition);
            assertFalse(response.getContentAsString().isBlank(),
                "invalid insertion should return an HTML error body: " + invalidPosition);
            assertEquals(before, editingStateFingerprint(controller), "state changed for " + invalidPosition);
            assertEquals(beforeCounter, counter(controller), "counter changed for " + invalidPosition);
            assertVary(response, "invalid insertion " + invalidPosition);
        }
    }

    @Test
    void insertionRetainsOriginalSpringIntegerBindingCompatibility() throws Exception {
        // These spellings were accepted by the original @PathVariable int controller.
        for (var entry : Map.ofEntries(Map.entry("0x0", 0), Map.entry("0X4", 4), Map.entry("#1", 1),
                Map.entry("-0x1", -1), Map.entry(" 1 ", 1), Map.entry("\t- 1\t", -1),
                Map.entry("+2", 2), Map.entry("04", 4)).entrySet()) {
            EditingDemoController controller = new EditingDemoController();
            var response = isolatedEditingMvc(controller)
                    .perform(post("/editing-demo/insert-row/{position}", entry.getKey())).andReturn().getResponse();
            assertEquals(200, response.getStatus(), entry.getKey());
            assertHtml(response, entry.getKey());
            assertVary(response, entry.getKey());
            assertEquals(6, rowCount(controller));
            assertEquals(101, counter(controller));
            assertEquals("row-101", rowId(controller, entry.getValue() + 1));
            assertTrue(response.getContentAsString().contains("/editing-demo/add-module/row-101"));
            List<?> rows = (List<?>) readField(pages(controller).get("demo-page"), "rows");
            for (int i = 0; i < rows.size(); i++) assertEquals(i, readField(rows.get(i), "position"));
        }
        for (String position : List.of("-0x1", "\t- 1\t")) {
            EditingDemoController controller = new EditingDemoController();
            clearRows(controller);
            var response = isolatedEditingMvc(controller)
                    .perform(post("/editing-demo/insert-row/{position}", position)).andReturn().getResponse();
            assertEquals(200, response.getStatus());
            assertEquals(1, rowCount(controller));
            assertEquals(101, counter(controller));
            assertEquals("row-101", rowId(controller, 0));
        }
    }

    @Test
    @DisplayName("A missing editing page is a controlled HTML 404")
    void missingEditingPageIsHtml404() throws Exception {
        EditingDemoController controller = new EditingDemoController();
        pages(controller).remove("demo-page");

        MockHttpServletResponse response = isolatedEditingMvc(controller)
            .perform(get("/editing-demo"))
            .andReturn().getResponse();

        assertEquals(404, response.getStatus());
        assertHtml(response, "missing editing page");
        assertTrue(response.getContentAsString().contains("Page not found"));
        assertVary(response, "missing editing page");
        int beforeCounter = counter(controller);
        for (var request : List.of(post("/editing-demo/insert-row/-1"), post("/editing-demo/insert-row/2147483648"), delete("/editing-demo/delete/module-1"))) {
            MockHttpServletResponse error = isolatedEditingMvc(controller).perform(request).andReturn().getResponse();
            assertEquals(404, error.getStatus());
            assertHtml(error, "missing editing page mutation");
            assertVary(error, "missing editing page mutation");
            assertEquals(beforeCounter, counter(controller));
            assertTrue(pages(controller).isEmpty());
        }
    }

    private static MockMvc isolatedEditingMvc(EditingDemoController controller) {
        return MockMvcBuilders.standaloneSetup(controller)
            .addFilters(new SeedVaryFilter())
            .build();
    }

    private static void assertHtml(MockHttpServletResponse response, String context) {
        assertNotNull(response.getContentType(), context + " must declare a content type");
        MediaType contentType = MediaType.parseMediaType(response.getContentType());
        assertTrue(MediaType.TEXT_HTML.isCompatibleWith(contentType), context + " must be text/html but was " + contentType);
    }

    private static void assertVary(MockHttpServletResponse response, String context) {
        Collection<String> varyHeaders = response.getHeaders("Vary");
        assertFalse(varyHeaders.isEmpty(), context + " must declare Vary");
        Set<String> tokens = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        varyHeaders.stream()
            .flatMap(header -> Arrays.stream(header.split(",")))
            .map(String::trim)
            .filter(token -> !token.isEmpty())
            .forEach(tokens::add);
        String vary = String.join(", ", varyHeaders);
        assertTrue(tokens.contains(UNRELATED_VARY_TOKEN), context + " discarded unrelated Vary token: " + vary);
        assertTrue(tokens.contains(HX_VARY_TOKEN), context + " must vary on HX-Request: " + vary);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> pages(EditingDemoController controller) throws Exception {
        return (Map<String, Object>) readField(controller, "pages");
    }

    private static int rowCount(EditingDemoController controller) throws Exception {
        Object page = pages(controller).get("demo-page");
        if (page == null) {
            return 0;
        }
        return ((List<?>) readField(page, "rows")).size();
    }

    private static String rowId(EditingDemoController controller, int index) throws Exception {
        Object page = pages(controller).get("demo-page");
        Object row = ((List<?>) readField(page, "rows")).get(index);
        return (String) readField(row, "id");
    }

    private static void clearRows(EditingDemoController controller) throws Exception {
        Object page = pages(controller).get("demo-page");
        ((List<?>) readField(page, "rows")).clear();
    }

    private static int counter(EditingDemoController controller) throws Exception {
        return ((AtomicInteger) readField(controller, "idCounter")).get();
    }

    private static String editingStateFingerprint(EditingDemoController controller) throws Exception {
        StringBuilder result = new StringBuilder();
        for (String pageId : new TreeSet<>(pages(controller).keySet())) {
            Object page = pages(controller).get(pageId);
            result.append("page=").append(pageId).append(';');
            List<?> rows = (List<?>) readField(page, "rows");
            for (Object row : rows) {
                result.append("row[")
                    .append(readField(row, "id")).append('|')
                    .append(readField(row, "position")).append('|')
                    .append(readField(row, "canAddModule")).append(']');
                for (Object module : (List<?>) readField(row, "modules")) {
                    result.append("module[")
                        .append(readField(module, "id")).append('|')
                        .append(readField(module, "title")).append('|')
                        .append(readField(module, "content")).append('|')
                        .append(readField(module, "width")).append('|')
                        .append(readField(module, "useMarkdown")).append('|')
                        .append(readField(module, "canEdit")).append('|')
                        .append(readField(module, "canDelete")).append('|')
                        .append(readField(module, "editMode")).append(']');
                }
            }
        }
        for (Object entries : ((Map<?, ?>) readField(controller, "pendingEdits")).values()) {
            for (Object pending : (List<?>) entries) {
                result.append("pending[").append(readField(pending, "moduleId")).append('|')
                    .append(readField(pending, "changes")).append('|')
                    .append(readField(pending, "timestamp")).append(']');
            }
        }
        return result.toString();
    }

    private static Object readField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private record HxCase(String name, String value, boolean selectsFragment) { }

    private record DocumentRoute(String name, Supplier<MockHttpServletRequestBuilder> request) { }

    private record FragmentRoute(String name, Supplier<MockHttpServletRequestBuilder> request) { }

    private record StatusRoute(String name, Supplier<MockHttpServletRequestBuilder> request, int status) { }

    private static final class SeedVaryFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
        ) throws ServletException, IOException {
            response.setHeader("Vary", UNRELATED_VARY_TOKEN);
            filterChain.doFilter(request, response);
        }
    }
}
