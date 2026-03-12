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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class BlogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Blog root should render first page with global shell and top nav")
    void blogRootRendersFirstPage() throws Exception {
        mockMvc.perform(get("/blog"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("SimplyPages Blog")))
            .andExpect(content().string(containsString("sp-content-list-item")))
            .andExpect(content().string(containsString("href=\"/blog\"")))
            .andExpect(content().string(containsString("href=\"/blog?page=2\"")))
            .andExpect(content().string(containsString("By Render Lab on 2026-03-10 08:30")))
            .andExpect(content().string(not(containsString("Testing Rendering Behavior Without Guesswork"))));
    }

    @Test
    @DisplayName("Blog pagination should resolve second index page")
    void blogPaginationRendersSecondPage() throws Exception {
        mockMvc.perform(get("/blog").param("page", "2"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Page 2 of 2")))
            .andExpect(content().string(containsString("Testing Rendering Behavior Without Guesswork")))
            .andExpect(content().string(containsString("href=\"/blog\"")));
    }

    @Test
    @DisplayName("Blog detail route should render markdown content with sticky TOC")
    void blogDetailRenders() throws Exception {
        mockMvc.perform(get("/blog/template-slotkey-render-context"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Template, SlotKey, and RenderContext in Real Projects")))
            .andExpect(content().string(containsString("By Render Lab on 2026-03-10 08:30")))
            .andExpect(content().string(containsString("sp-content-toc-item")))
            .andExpect(content().string(containsString("Keep Dynamic Values in Context")));
    }

    @Test
    @DisplayName("Unknown blog slug should return not found status")
    void unknownBlogSlugReturnsNotFound() throws Exception {
        mockMvc.perform(get("/blog/does-not-exist"))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Blog content not found")));
    }
}
