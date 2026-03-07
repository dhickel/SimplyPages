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
            .andExpect(content().string(containsString("Basics &amp; Forms")))
            .andExpect(content().string(containsString("Display &amp; Data")))
            .andExpect(content().string(containsString("Modules")))
            .andExpect(content().string(containsString("HTMX &amp; Editing")));
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
}
