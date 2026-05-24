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
}
