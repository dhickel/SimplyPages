package io.mindspice.demo;

import io.mindspice.demo.chat.ChatDemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class SandboxSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired SandboxRegistry registry;
    @Autowired ChatDemoService chat;

    private MockHttpSession visitor(String page) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get(page).session(session)).andExpect(status().isOk());
        assertNotNull(session.getAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE));
        return session;
    }

    private String token(MockHttpSession session) {
        return (String) session.getAttribute(SandboxSecurityFilter.TOKEN_ATTRIBUTE);
    }

    private MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, MockHttpSession session) {
        return request.session(session).header("Origin", "http://localhost")
            .header(SandboxSecurityFilter.TOKEN_HEADER, token(session));
    }

    private SandboxRegistry.Entry entry(MockHttpSession session) {
        SandboxRegistry.Entry entry = registry.acquire(session, false, false);
        registry.release(entry, session, false);
        return entry;
    }

    @Test void failedInitialNegotiationDoesNotStartASandboxAndCanRetryNormally() throws Exception {
        for (String page : List.of("/editing-demo", "/forum")) {
            MockHttpSession session = new MockHttpSession();
            try {
                mvc.perform(get(page).session(session).accept("application/json"))
                    .andExpect(status().isNotAcceptable());
                assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
                assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class,
                    () -> registry.acquire(session, false, false)).getStatusCode().value());
                mvc.perform(get(page).session(session)).andExpect(status().isOk());
                SandboxRegistry.Entry committed = entry(session);
                mvc.perform(get(page).session(session).accept("application/json"))
                    .andExpect(status().isNotAcceptable());
                assertSame(committed, entry(session));
                assertEquals(Boolean.TRUE, session.getAttribute(SandboxRegistry.SESSION_STARTED));
            } finally { registry.sessionDestroyed(session.getId()); }
        }
    }

    @Test void editingContentProposalsPermissionsAndIdsAreVisitorLocal() throws Exception {
        MockHttpSession owner = visitor("/editing-demo"), peer = visitor("/editing-demo");
        try {
            mvc.perform(write(post("/editing-demo/save-child/module-9/item-0"), owner)
                .param("text", "Owned proposal").param("editMode", "USER_EDIT")).andExpect(status().isOk());
            mvc.perform(get("/editing-demo/pending-edits").session(owner))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Module: module-9")));
            mvc.perform(get("/editing-demo/pending-edits").session(peer))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No pending edits")));
            mvc.perform(write(post("/editing-demo/pending-edits/module-9/approve"), peer)).andExpect(status().isNotFound());
            mvc.perform(write(post("/editing-demo/pending-edits/module-9/approve"), owner)).andExpect(status().isOk());
            mvc.perform(get("/editing-demo").session(peer))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Owned proposal"))));
            mvc.perform(get("/editing-demo").session(owner))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Owned proposal")));
            mvc.perform(write(post("/editing-demo/insert-row/999"), owner)).andExpect(status().isBadRequest());
            mvc.perform(write(post("/editing-demo/insert-row/0"), owner)).andExpect(status().isOk());
            mvc.perform(get("/editing-demo/add-module-modal/row-101").session(peer)).andExpect(status().isNotFound());
            mvc.perform(write(post("/editing-demo/insert-row/0"), peer)).andExpect(status().isOk());
            mvc.perform(get("/editing-demo/add-module-modal/row-101").session(peer)).andExpect(status().isOk());
            mvc.perform(write(post("/editing-demo/save/module-3"), owner).param("content", "forbidden"))
                .andExpect(status().isForbidden());
            mvc.perform(write(delete("/editing-demo/delete/module-1"), owner)).andExpect(status().isOk());
            mvc.perform(get("/editing-demo/edit/module-1").session(peer)).andExpect(status().isOk());
        } finally { registry.sessionDestroyed(owner.getId()); registry.sessionDestroyed(peer.getId()); }
    }

    @Test void forumPersonaAndModeratorNeverCrossVisitorBoundary() throws Exception {
        MockHttpSession owner = visitor("/forum"), peer = visitor("/forum");
        try {
            String title = "Only my visitor " + UUID.randomUUID();
            mvc.perform(write(post("/forum/topics/create"), owner).param("scope", "cat-feedback")
                .param("title", title).param("body", "Private body")).andExpect(status().isOk());
            var topic = entry(owner).forum.listAllTopicsNewestFirst().stream().filter(t -> t.title().equals(title)).findFirst().orElseThrow();
            mvc.perform(write(post("/forum/viewer"), peer).param("displayName", "Same Persona")
                .param("userId", topic.ownerId()).param("role", "moderator")).andExpect(status().isOk());
            mvc.perform(get("/forum/topics").session(peer).param("scope", "cat-feedback"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(title))));
            for (String action : List.of("edit", "quote", "update", "delete")) {
                mvc.perform(write(post("/forum/topics/" + topic.id() + "/" + action), peer)
                    .param("title", "foreign").param("body", "foreign")).andExpect(status().isNotFound());
            }
            assertEquals(title, entry(owner).forum.findTopic(topic.id()).orElseThrow().title());
            assertTrue(entry(peer).forum.findTopic(topic.id()).isEmpty());
            mvc.perform(write(post("/forum/comments/create"), owner).param("topicId", topic.id())
                .param("body", "Owned comment")).andExpect(status().isOk());
            var comment = entry(owner).forum.listAllCommentsOldestFirst().stream()
                .filter(c -> c.topicId().equals(topic.id())).findFirst().orElseThrow();
            for (String action : List.of("edit", "quote", "update", "delete")) {
                mvc.perform(write(post("/forum/comments/" + comment.id() + "/" + action), peer)
                    .param("body", "foreign")).andExpect(status().isNotFound());
            }
        } finally { registry.sessionDestroyed(owner.getId()); registry.sessionDestroyed(peer.getId()); }
    }

    @Test void everyMutationRouteRejectsWithoutSessionOrAllocationBeforeMvc() throws Exception {
        for (String route : List.of("/editing-demo/save/module-1", "/editing-demo/insert-row/0",
            "/editing-demo/add-module/row-1", "/editing-demo/pending-edits/module-2/approve",
            "/editing-demo/save-child/module-9/item-0", "/forum/viewer", "/forum/topics/create",
            "/forum/comments/create", "/forum/topics/topic-0001/edit", "/forum/topics/topic-0001/quote",
            "/forum/topics/topic-0001/update", "/forum/topics/topic-0001/delete",
            "/forum/comments/comment-00001/edit", "/forum/comments/comment-00001/quote",
            "/forum/comments/comment-00001/update", "/forum/comments/comment-00001/delete",
            "/chat/messages", "/chat/new", "/sandbox/new", "/demos/api/form-preview", "/demos/api/template-card")) {
            var result = mvc.perform(post(route).header("Origin", "http://localhost"))
                .andExpect(status().isForbidden()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
            assertNull(result.getRequest().getSession(false), route);
        }
        for (String route : List.of("/editing-demo/delete/module-1", "/editing-demo/delete-child/module-9/item-0",
            "/editing-demo/pending-edits/module-2/reject")) {
            var result = mvc.perform(delete(route)).andExpect(status().isForbidden()).andReturn();
            assertNull(result.getRequest().getSession(false));
        }
        for (String route : List.of("/forum;matrix/topics/create", "/%66orum/topics/create", "/editing-demo;v/save/module-1")) {
            mvc.perform(post(URI.create(route))).andExpect(status().isForbidden());
        }
    }

    @Test void invalidOriginOrTokenCannotMutateEditingForumChatOrAllocateSandbox() throws Exception {
        MockHttpSession owner = visitor("/editing-demo"), peer = visitor("/sandbox/token");
        try {
            mvc.perform(get("/chat").session(owner)).andExpect(status().isOk());
            String chatId = (String) owner.getAttribute(ChatDemoService.SESSION_CONVERSATION);
            var originalEntry = entry(owner);
            String before = mvc.perform(get("/editing-demo/edit/module-1").session(owner)).andReturn().getResponse().getContentAsString()
                .replaceAll("[0-9a-f]{8}-[0-9a-f-]{27,}", "id");
            int topics = entry(owner).forum.listAllTopicsNewestFirst().size();
            for (String origin : new String[]{null, "null", "https://localhost", "http://localhost:81", "http://foreign.test", "http://localhost/", "http://localhost@foreign.test", "http://localhost http://foreign.test"}) {
                var request = post("/editing-demo/save/module-1").session(owner).param("title", "must not appear")
                    .header(SandboxSecurityFilter.TOKEN_HEADER, token(owner));
                if (origin != null) request.header("Origin", origin);
                mvc.perform(request).andExpect(status().isForbidden());
            }
            for (String route : List.of("/editing-demo/save/module-2", "/forum/topics/create", "/chat/messages", "/sandbox/new")) {
                mvc.perform(post(route).session(owner).header("Origin", "http://localhost")
                    .header(SandboxSecurityFilter.TOKEN_HEADER, token(peer)).param("title", "must not appear")
                    .param("content", "must not appear").param("scope", "cat-feedback").param("body", "must not appear")
                    .param("message", "must not appear").param("conversationId", chatId)).andExpect(status().isForbidden());
            }
            mvc.perform(write(post("/forum/topics/create"), owner).header("Sec-Fetch-Site", "same-site"))
                .andExpect(status().isForbidden());
            mvc.perform(write(post("/forum/topics/create"), owner).header("Origin", "http://foreign.test"))
                .andExpect(status().isForbidden());
            mvc.perform(post("/editing-demo/save/module-1").session(owner).header("Origin", "http://localhost")
                .param("_csrf", token(owner), token(owner))).andExpect(status().isForbidden());
            assertSame(originalEntry, entry(owner));
            assertEquals(before, mvc.perform(get("/editing-demo/edit/module-1").session(owner)).andReturn().getResponse().getContentAsString()
                .replaceAll("[0-9a-f]{8}-[0-9a-f-]{27,}", "id"));
            mvc.perform(get("/editing-demo/pending-edits").session(owner))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("No pending edits")));
            assertEquals(topics, entry(owner).forum.listAllTopicsNewestFirst().size());
            assertEquals(1, chat.history(owner, chatId).cursor());
            assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> entry(peer));
        } finally {
            registry.sessionDestroyed(owner.getId()); registry.sessionDestroyed(peer.getId()); chat.sessionDestroyed(owner.getId());
        }
    }

    @Test void editingRowAndChildContentBoundsRejectWithoutConsumingIdsOrChangingContent() throws Exception {
        MockHttpSession session = visitor("/editing-demo");
        try {
            mvc.perform(write(post("/editing-demo/save-child/module-9/item-0"), session)
                .param("text", "x".repeat(5001))).andExpect(status().isBadRequest())
                .andExpect(header().string("X-Editing-Demo-Modal", "true"));
            mvc.perform(get("/editing-demo/edit-child/module-9/item-0").session(session))
                .andExpect(content().string(org.hamcrest.Matchers.containsString(">Item 1</textarea>")));
            for (int row = 5; row < 64; row++) {
                mvc.perform(write(post("/editing-demo/insert-row/0"), session)).andExpect(status().isOk());
            }
            mvc.perform(write(post("/editing-demo/insert-row/0"), session)).andExpect(status().isConflict());
            mvc.perform(write(delete("/editing-demo/delete/module-9"), session)).andExpect(status().isOk());
            mvc.perform(write(post("/editing-demo/insert-row/0"), session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/editing-demo/add-module/row-160")));
        } finally { registry.sessionDestroyed(session.getId()); }
    }

    @Test void demoPresentationTokenSupportsNativeAndHtmxApiPostsWithoutSandboxAdmission() throws Exception {
        var page = mvc.perform(get("/demos")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store")).andReturn();
        MockHttpSession session = (MockHttpSession) page.getRequest().getSession(false);
        assertNotNull(session);
        String token = SandboxSecurityFilter.requestToken(page.getRequest());
        assertEquals(token, page.getResponse().getHeader(SandboxSecurityFilter.TOKEN_HEADER));
        assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
        assertNull(session.getAttribute(ChatDemoService.SESSION_CONVERSATION));
        mvc.perform(post("/demos/api/form-preview").session(session)
            .header("Referer", "http://localhost/demos/components/forms")
            .param(SandboxSecurityFilter.TOKEN_PARAMETER, token).param("name", "Native <form>"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Native &lt;form&gt;")));
        mvc.perform(write(post("/demos/api/template-card"), session)
            .header("HX-Request", "true").param("title", "Header title").param("body", "Header body"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Header title")));
        mvc.perform(write(post("/demos/api/form-preview"), session).param("_csrf", "wrong"))
            .andExpect(status().isForbidden());
        assertEquals(token, token(session));
        assertNull(session.getAttribute(SandboxRegistry.SESSION_STARTED));
        assertEquals(404, assertThrows(org.springframework.web.server.ResponseStatusException.class,
            () -> registry.acquire(session, false, false)).getStatusCode().value());
    }

    @Test void tokenDeliveryNativeFormRefererAndHeaderContract() throws Exception {
        MockHttpSession session = visitor("/sandbox/token");
        try {
            String token = token(session);
            assertEquals(43, token.length());
            mvc.perform(get("/sandbox/token").session(session)).andExpect(status().isOk())
                .andExpect(header().string(SandboxSecurityFilter.TOKEN_HEADER, token))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"parameter\":\"_csrf\"")));
            mvc.perform(post("/sandbox/new").session(session).header("Referer", "http://localhost/demos?x=1")
                .param("_csrf", token)).andExpect(status().isNoContent());
            mvc.perform(write(post("/forum/viewer"), session).param("displayName", "Reset persona")
                .param("userId", "reset-persona").param("role", "moderator")).andExpect(status().isOk());
            mvc.perform(write(post("/sandbox/new"), session)).andExpect(status().isNoContent());
            assertNull(session.getAttribute(ForumDemoController.SESSION_VIEWER_ID));
            assertNull(session.getAttribute(ForumDemoController.SESSION_VIEWER_NAME));
            assertNull(session.getAttribute(ForumDemoController.SESSION_VIEWER_MODERATOR));
            mvc.perform(write(post("/editing-demo/save/module-4"), session).param("title", "Header save"))
                .andExpect(status().isOk());
            mvc.perform(post("/editing-demo/save/module-4").session(session).header("Origin", "http://localhost")
                .param("_csrf", token).param("title", "Native form save")).andExpect(status().isOk());
            var rejected = mvc.perform(get("/sandbox/token").header("Origin", "http://foreign.test"))
                .andExpect(status().isForbidden()).andReturn();
            assertNull(rejected.getRequest().getSession(false));
            var hx = mvc.perform(get("/chat").header("HX-Request", "TrUe")).andExpect(status().isOk()).andReturn();
            assertNull(hx.getRequest().getSession(false));
        } finally { registry.sessionDestroyed(session.getId()); }
    }
}
