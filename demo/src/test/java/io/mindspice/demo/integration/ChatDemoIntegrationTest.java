package io.mindspice.demo.integration;

import io.mindspice.demo.ChatDemoController;
import io.mindspice.demo.chat.ChatDemoService;
import io.mindspice.demo.pages.ChatDemoPage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatDemoIntegrationTest {
    private ChatDemoService service;
    private MockMvc mvc;
    @BeforeEach void freshState() {
        service = new ChatDemoService();
        mvc = MockMvcBuilders.standaloneSetup(new ChatDemoController(service, new ChatDemoPage())).build();
    }
    @AfterEach void cleanup() { service.close(); }

    private MockHttpSession initialize() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(get("/chat").session(session)).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store")).andExpect(header().string("Vary", "HX-Request"));
        return session;
    }
    private String id(MockHttpSession session) { return (String) session.getAttribute(ChatDemoService.SESSION_CONVERSATION); }

    @Test void normalPageAndActualGeneratedRecoveryControlsAndBoundedComposer() throws Exception {
        var result = mvc.perform(get("/chat")).andExpect(status().isOk()).andReturn();
        String html = result.getResponse().getContentAsString();
        assertTrue(html.contains("id=\"chat-main\""));
        assertTrue(html.contains("data-sp-chat=\"true\""));
        assertTrue(html.contains("src=\"/js/chat-demo.js\""));
        assertTrue(html.contains("id=\"chat-resume\""));
        assertTrue(html.contains("id=\"chat-new\""));
        assertTrue(html.contains("maxlength=\"4096\""));
        assertTrue(html.contains("Session ownership is not authentication"));
        assertTrue(html.contains("25 complete exchanges"));
    }

    @Test void exactTrueHxRedirectDoesNotAllocateEvenASession() throws Exception {
        for (String hx : List.of("true", "TrUe")) {
            MvcResult result = mvc.perform(get("/chat").header("HX-Request", hx)).andExpect(status().isOk())
                .andExpect(header().string("HX-Redirect", "/chat")).andExpect(content().string(""))
                .andExpect(header().string("Cache-Control", "no-store")).andReturn();
            assertNull(result.getRequest().getSession(false));
        }
        for (String hx : List.of("false", " true ", "junk")) mvc.perform(get("/chat").header("HX-Request", hx))
            .andExpect(status().isOk()).andExpect(header().doesNotExist("HX-Redirect"));
    }

    @Test void ownedHistoryMessageAndStreamRequireAnInitializedExactSession() throws Exception {
        MockHttpSession owner = initialize(), peer = initialize();
        String id = id(owner);
        mvc.perform(post("/chat/messages").session(owner).param("conversationId", id).param("message", "Integration hello"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        String history = mvc.perform(get("/chat/history").session(owner).param("conversationId", id)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertTrue(history.contains("Received: Integration hello"));
        assertFalse(history.contains("<!DOCTYPE html>"));
        for (String bad : List.of(id, "unknown", " " + id, id + " ")) {
            mvc.perform(get("/chat/history").session(peer).param("conversationId", bad)).andExpect(status().isNotFound());
            mvc.perform(post("/chat/messages").session(peer).param("conversationId", bad).param("message", "foreign"))
                .andExpect(status().isNotFound());
            mvc.perform(get("/chat/stream").session(peer).param("conversationId", bad))
                .andExpect(status().isNotFound()).andExpect(request().asyncNotStarted());
        }
        assertEquals(3, service.history(owner, id).messages().size());
        mvc.perform(get("/chat/stream").session(owner).param("conversationId", id))
            .andExpect(status().isOk()).andExpect(request().asyncStarted()).andExpect(content().contentTypeCompatibleWith("text/event-stream"));
        mvc.perform(get("/chat/stream").session(owner).param("conversationId", id)).andExpect(request().asyncStarted());
        mvc.perform(get("/chat/stream").session(owner).param("conversationId", id)).andExpect(status().isTooManyRequests());
    }

    @Test void missingBlankUnknownApiCallsNeverCreateSessionsAndAlwaysNoStore() throws Exception {
        for (String action : List.of("history", "messages", "stream")) {
            for (String value : new String[]{null, "", " ", "unknown"}) {
                var request = action.equals("messages") ? post("/chat/" + action).param("message", "attempt") : get("/chat/" + action);
                if (value != null) request.param("conversationId", value);
                var result = mvc.perform(request).andExpect(status().is(value == null || value.isBlank() ? 400 : 404))
                    .andExpect(header().string("Cache-Control", "no-store")).andReturn();
                assertNull(result.getRequest().getSession(false));
                assertFalse(result.getRequest().isAsyncStarted());
            }
        }
    }

    @Test void invalidPretrimMessageDoesNotAppendAndExplicitNewIsOnlyReset() throws Exception {
        MockHttpSession owner = initialize();
        String original = id(owner);
        for (String invalid : List.of(" ", " " + "x".repeat(4096), "😀".repeat(2048) + "x")) {
            mvc.perform(post("/chat/messages").session(owner).param("conversationId", original).param("message", invalid))
                .andExpect(status().isBadRequest());
        }
        assertEquals(1, service.history(owner, original).cursor());
        mvc.perform(post("/chat/messages").session(owner).param("conversationId", original).param("message", "😀".repeat(2048)))
            .andExpect(status().isOk());
        mvc.perform(post("/chat/new").session(owner)).andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
        assertNotEquals(original, id(owner));
        assertEquals(1, service.history(owner, id(owner)).messages().size());
        mvc.perform(get("/chat/history").session(owner).param("conversationId", original)).andExpect(status().isNotFound());
    }
}
