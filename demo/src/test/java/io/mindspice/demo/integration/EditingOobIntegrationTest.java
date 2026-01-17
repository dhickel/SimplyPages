package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class EditingOobIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Phase 6.5 save should return OOB swaps for modal + page content")
    void phase6_5SaveReturnsOobSwaps() throws Exception {
        mockMvc.perform(post("/test/phase6-5/save/module-3")
                .param("title", "Updated Title")
                .param("content", "Updated Content")
                .param("width", "6"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"edit-modal-container\"")))
            .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"page-content\"")));
    }

    @Test
    @DisplayName("Phase 6.5 add module should return OOB swaps for modal + page content")
    void phase6_5AddModuleReturnsOobSwaps() throws Exception {
        mockMvc.perform(post("/test/phase6-5/add-module/row-4")
                .param("title", "New Module")
                .param("content", "Module Content")
                .param("width", "12"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"edit-modal-container\"")))
            .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"page-content\"")));
    }

    @Test
    @DisplayName("Phase 8 update should return OOB swaps for modal + refreshed container")
    void phase8UpdateReturnsOobSwaps() throws Exception {
        mockMvc.perform(post("/test/phase8/api/modules/module-1/update")
                .param("user", "admin")
                .param("title", "Public Content")
                .param("content", "Updated by admin")
                .param("useMarkdown", "on"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"edit-modal-container\"")))
            .andExpect(content().string(containsString("hx-swap-oob=\"outerHTML\" class=\"container\"")));
    }

    @Test
    @DisplayName("Phase 8 delete should return OOB swaps for modal + refreshed container")
    void phase8DeleteReturnsOobSwaps() throws Exception {
        mockMvc.perform(delete("/test/phase8/api/modules/module-2/delete")
                .param("user", "admin"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"edit-modal-container\"")))
            .andExpect(content().string(containsString("hx-swap-oob=\"outerHTML\" class=\"container\"")));
    }
}
