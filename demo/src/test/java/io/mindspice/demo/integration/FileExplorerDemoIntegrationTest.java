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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class FileExplorerDemoIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Explorer and picker demo routes render")
    void demoRoutesRender() throws Exception {
        mockMvc.perform(get("/demos/file-explorer")).andExpect(status().isOk()).andExpect(content().string(containsString("Demo File Explorer")));
        mockMvc.perform(get("/demos/file-picker")).andExpect(status().isOk()).andExpect(content().string(containsString("Demo File Picker")));
    }

    @Test
    @DisplayName("Picker select endpoint returns target fragment")
    void pickerSelectReturnsFragment() throws Exception {
        mockMvc.perform(post("/demos/file-picker/select").param("path", "/workspace/docs/readme.md"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"demo-file-picker-value\"")));
    }

    @Test
    @DisplayName("Explorer fragment endpoints return matching target ids")
    void explorerFragmentsUseStableIds() throws Exception {
        mockMvc.perform(get("/demos/file-explorer/list").param("path", "/workspace/docs"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"demo-file-explorer-list\"")))
            .andExpect(content().string(containsString("readme.md")))
            .andExpect(content().string(containsString("notes.txt")))
            .andExpect(content().string(containsString("images")));
        mockMvc.perform(get("/demos/file-explorer/inspect").param("path", "/workspace/docs/readme.md"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"demo-file-explorer-inspector\"")));
        mockMvc.perform(get("/demos/file-explorer/view").param("path", "/workspace/docs/readme.md"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"demo-file-explorer-viewer\"")));
    }

    @Test
    @DisplayName("Picker fragment endpoints return matching target ids")
    void pickerFragmentsUseStableIds() throws Exception {
        mockMvc.perform(get("/demos/file-picker/list").param("path", "/workspace"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"demo-file-picker-list\"")))
            .andExpect(content().string(containsString("docs")))
            .andExpect(content().string(containsString("images")));
        mockMvc.perform(get("/demos/file-picker/inspect").param("path", "/workspace/images"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"demo-file-picker-inspector\"")));
        mockMvc.perform(get("/demos/file-picker/view").param("path", "/workspace/docs/readme.md"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"demo-file-picker-viewer\"")));
    }

    @Test
    @DisplayName("Modal and action endpoints render HTMX fragments")
    void modalAndActionEndpointsRender() throws Exception {
        mockMvc.perform(get("/demos/file-explorer/modal/delete").param("path", "/workspace/docs/readme.md"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"demo-file-explorer-modal\"")));
        mockMvc.perform(get("/demos/file-explorer/modal/delete").param("path", "/workspace/images"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Confirm recursive delete")));
        mockMvc.perform(get("/demos/file-explorer/modal/rename").param("path", "/workspace/docs/readme.md"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Demo rename")));
        mockMvc.perform(get("/demos/file-picker/modal/delete").param("path", "/workspace/images"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("id=\"demo-file-picker-modal\"")));
        mockMvc.perform(get("/demos/file-explorer/action/copy").param("path", "/workspace/docs/readme.md"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Ran copy action for: /workspace/docs/readme.md")));
    }

    @Test
    @DisplayName("Picker demo modes render appropriate selection state")
    void pickerModesRender() throws Exception {
        mockMvc.perform(get("/demos/file-picker").param("mode", "DIRECTORIES"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("hx-post=\"/demos/file-picker/select?path=%2Fworkspace%2Fimages\"")));
        mockMvc.perform(get("/demos/file-picker").param("mode", "FILES"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(containsString("hx-post=\"/demos/file-picker/select?path=%2Fworkspace%2Fimages\""))));
    }
}
