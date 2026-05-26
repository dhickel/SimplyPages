package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class EditingOobIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Demos shell should render consolidated sidebar and top nav")
    void demosShellRendersConsolidatedNavigation() throws Exception {
        mockMvc.perform(get("/demos"))
            .andExpect(status().isOk())
            .andExpect(header().string("Vary", containsString("HX-Request")))
            .andExpect(content().string(containsString("Home")))
            .andExpect(content().string(containsString("Javadocs")))
            .andExpect(content().string(containsString("Forum")))
            .andExpect(content().string(containsString("Chat")))
            .andExpect(content().string(containsString("Docs")))
            .andExpect(content().string(containsString("Basics &amp; Forms")))
            .andExpect(content().string(containsString("Display &amp; Data")))
            .andExpect(content().string(containsString("Modules")))
            .andExpect(content().string(containsString("HTMX &amp; Editing")))
            .andExpect(content().string(containsString(">Chat<")));
    }

    @Test
    @DisplayName("Legacy flat route should be removed")
    void legacyRoutesAreRemoved() throws Exception {
        mockMvc.perform(get("/components"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("HTMX request should return page fragment without shell")
    void htmxRequestReturnsFragment() throws Exception {
        mockMvc.perform(get("/demos/modules").header("HX-Request", "true"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Modules Library")))
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<!DOCTYPE html>"))));
    }

    @Test
    @DisplayName("Template API should return rendered module fragment")
    void templateEndpointRendersCardModule() throws Exception {
        mockMvc.perform(post("/demos/api/template-card")
                .param("title", "Updated")
                .param("body", "Body value"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("dynamic-card-preview")))
            .andExpect(content().string(containsString("Updated")))
            .andExpect(content().string(containsString("Body value")));
    }

    @Test
    @DisplayName("Modules demo should render assistant workspace primitives and fragments")
    void assistantWorkspaceDemoRendersFragments() throws Exception {
        mockMvc.perform(get("/demos/modules"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Assistant Workspace Primitives")))
            .andExpect(content().string(containsString("assistant-chat-module")))
            .andExpect(content().string(containsString("chat-room-list-module")))
            .andExpect(content().string(containsString("timeline-transcript-module")))
            .andExpect(content().string(containsString("master-detail-browser-module")))
            .andExpect(content().string(containsString("polling-panel")))
            .andExpect(content().string(containsString("htmx-tab-nav")))
            .andExpect(content().string(containsString("/demos/api/assistant/room?room=handoff")))
            .andExpect(content().string(containsString("/demos/api/workspace/detail?item=runs")));

        mockMvc.perform(get("/demos/api/assistant/room").param("room", "review"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("assistant-room-fragment")))
            .andExpect(content().string(containsString("Review Room")))
            .andExpect(content().string(containsString("Validator should inspect module composition")));

        mockMvc.perform(get("/demos/api/assistant/status"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("assistant-status-panel")))
            .andExpect(content().string(containsString("hx-get=\"/demos/api/assistant/status\"")))
            .andExpect(content().string(containsString("Polling")));

        mockMvc.perform(get("/demos/api/workspace/detail").param("item", "runs"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Runs")))
            .andExpect(content().string(containsString("Complete")));
    }
}
