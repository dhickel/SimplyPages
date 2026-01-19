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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class EditingDemoNestedEditingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should show add item button in nested module edit modal")
    void shouldShowAddItemButton() throws Exception {
        // Module 9 is the nested editing demo module
        mockMvc.perform(get("/editing-demo/edit/module-9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add Item")))
                .andExpect(content().string(containsString("hx-get=\"/editing-demo/add-child/module-9\"")));
    }

    @Test
    @DisplayName("Should return add child modal")
    void shouldReturnAddChildModal() throws Exception {
        mockMvc.perform(get("/editing-demo/add-child/module-9"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add New Item")))
                .andExpect(content().string(containsString("hx-post=\"/editing-demo/add-child/module-9\"")));
    }

    @Test
    @DisplayName("Should add child and return OOB update")
    void shouldAddChild() throws Exception {
        mockMvc.perform(post("/editing-demo/add-child/module-9")
                        .param("text", "New Test Item"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"edit-modal-container\"")))
                .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"page-content\"")))
                .andExpect(content().string(containsString("New Test Item")));
    }

    @Test
    @DisplayName("Should return edit child modal")
    void shouldReturnEditChildModal() throws Exception {
        // Assuming item-0 exists (initialized in controller)
        mockMvc.perform(get("/editing-demo/edit-child/module-9/item-0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Edit Item")))
                .andExpect(content().string(containsString("Item Text")))
                .andExpect(content().string(containsString("hx-post=\"/editing-demo/save-child/module-9/item-0\"")));
    }

    @Test
    @DisplayName("Should save child and return OOB update")
    void shouldSaveChild() throws Exception {
        mockMvc.perform(post("/editing-demo/save-child/module-9/item-0")
                        .param("text", "Updated Item Text"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"edit-modal-container\"")))
                .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"page-content\"")))
                .andExpect(content().string(containsString("Updated Item Text")));
    }

    @Test
    @DisplayName("Should delete child and return OOB update")
    void shouldDeleteChild() throws Exception {
        // Create a new item to delete to avoid affecting other tests order if parallel
        mockMvc.perform(post("/editing-demo/add-child/module-9").param("text", "Delete Me"));

        // We don't know the exact index, but we can try to delete the last one.
        // Or just delete item-0 and verify it's gone.

        mockMvc.perform(delete("/editing-demo/delete-child/module-9/item-0"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"edit-modal-container\"")))
                .andExpect(content().string(containsString("hx-swap-oob=\"true\" id=\"page-content\"")));
    }
}
