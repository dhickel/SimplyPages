package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
    @DisplayName("Editing demo should enforce module permissions at endpoints")
    void editingDemoEnforcesEndpointPermissions() throws Exception {
        mockMvc.perform(get("/editing-demo/edit/module-3"))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString("cannot be edited")));

        mockMvc.perform(post("/editing-demo/save/module-3")
                .param("title", "Blocked")
                .param("content", "Blocked"))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString("cannot be edited")));

        mockMvc.perform(delete("/editing-demo/delete/module-4"))
            .andExpect(status().isForbidden())
            .andExpect(content().string(containsString("cannot be deleted")));
    }

    @Test
    @DisplayName("Editing demo should reject invalid row insert bounds")
    void editingDemoRejectsInvalidInsertBounds() throws Exception {
        mockMvc.perform(post("/editing-demo/insert-row/99"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Invalid row insert position")));
    }

    @Test
    @DisplayName("Editing demo should expose and handle nested child edit endpoints")
    void editingDemoNestedChildEndpointsWork() throws Exception {
        mockMvc.perform(get("/editing-demo/edit/module-9"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/editing-demo/edit-child/module-9/item-0")))
            .andExpect(content().string(containsString("/editing-demo/delete-child/module-9/item-0")));

        mockMvc.perform(get("/editing-demo/edit-child/module-9/item-0"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Edit List Item")))
            .andExpect(content().string(containsString("value=\"Item 1\"")));

        mockMvc.perform(post("/editing-demo/save-child/module-9/item-0")
                .param("text", "Updated Item"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Updated Item")));
    }

    @Test
    @DisplayName("Editing demo should rely on renderer escaping instead of pre-escaping")
    void editingDemoDoesNotDoubleEscapeSavedText() throws Exception {
        mockMvc.perform(post("/editing-demo/save/module-4")
                .param("title", "<b>Safe</b>")
                .param("content", "<i>Body</i>")
                .param("width", "4"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("&lt;b&gt;Safe&lt;/b&gt;")))
            .andExpect(content().string(not(containsString("&amp;lt;b&amp;gt;Safe&amp;lt;/b&amp;gt;"))));
    }
}
