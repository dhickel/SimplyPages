package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class ChatDemoIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Chat page should render in full shell")
    void chatPageRenders() throws Exception {
        mockMvc.perform(get("/chat"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"chat-main\"")))
            .andExpect(content().string(containsString("chat-module")))
            .andExpect(content().string(containsString("data-sp-chat=\"true\"")))
            .andExpect(content().string(containsString("src=\"/js/chat-demo.js\"")))
            .andExpect(content().string(containsString("href=\"/blog\"")))
            .andExpect(content().string(containsString("href=\"/chat\"")))
            .andExpect(header().string("Vary", "HX-Request"));
    }

    @Test
    @DisplayName("An HTMX chat navigation should request the full shell")
    void htmxChatNavigationRedirectsToFullShell() throws Exception {
        mockMvc.perform(get("/chat").header("HX-Request", "TrUe"))
            .andExpect(status().isOk())
            .andExpect(header().string("HX-Redirect", "/chat"))
            .andExpect(header().string("Vary", "HX-Request"))
            .andExpect(content().string(emptyString()))
            .andExpect(content().string(not(containsString("data-sp-chat=\"true\""))));
    }

    @Test
    @DisplayName("A non-true HX header should retain the full chat shell contract")
    void falseHxChatRequestRendersFullShell() throws Exception {
        mockMvc.perform(get("/chat").header("HX-Request", "false"))
            .andExpect(status().isOk())
            .andExpect(header().string("Vary", "HX-Request"))
            .andExpect(content().string(containsString("data-sp-chat=\"true\"")))
            .andExpect(content().string(containsString("src=\"/js/chat-demo.js\"")))
            .andExpect(header().doesNotExist("HX-Redirect"));
    }

    @Test
    @DisplayName("Chat history endpoint should return fragment output")
    void historyEndpointRendersFragment() throws Exception {
        mockMvc.perform(get("/chat/history")
                .header("HX-Request", "true")
                .param("conversationId", "conv-fragment"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"chat-history\"")))
            .andExpect(content().string(not(containsString("<!DOCTYPE html>"))));
    }

    @Test
    @DisplayName("Posting a message should update transcript with user and assistant entries")
    void postMessageUpdatesTranscript() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/chat/messages")
                .session(session)
                .header("HX-Request", "true")
                .param("conversationId", "conv-integration")
                .param("message", "Integration hello"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Integration hello")))
            .andExpect(content().string(containsString("Received: Integration hello")));
    }

    @Test
    @DisplayName("SSE stream endpoint should open async event-stream response")
    void streamEndpointOpensSse() throws Exception {
        mockMvc.perform(get("/chat/stream")
                .param("conversationId", "conv-sse"))
            .andExpect(request().asyncStarted())
            .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM));
    }
}
