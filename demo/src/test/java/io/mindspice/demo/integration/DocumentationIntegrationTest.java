package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import io.mindspice.demo.DocumentationService;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class DocumentationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired DocumentationService docs;

    @Test void docsLinksAndFragmentsHaveCanonicalTargets() throws Exception {
        var full = mvc.perform(get("/docs")).andExpect(status().isOk()).andReturn().getResponse();
        var doc = Jsoup.parse(full.getContentAsString());
        assertEquals(1, doc.select("#docs-content").size());
        assertFalse(doc.select("#docs-content a[href^=/docs/]").isEmpty());
        assertEquals("outerHTML", doc.selectFirst(".docs-nav [hx-get]").attr("hx-swap"));
        var fragment = mvc.perform(get("/docs/core/03-template-rendercontext-slotkey-reference.md")
            .header("HX-Request", "true")).andExpect(status().isOk()).andReturn().getResponse();
        doc = Jsoup.parse(fragment.getContentAsString());
        assertEquals(1, doc.select("#docs-content").size());
        assertEquals(1, doc.select("h1").size());
        assertNotNull(doc.getElementById("slotkey"));
        assertEquals("HX-Request", fragment.getHeader("Vary"));
    }

    @Test void missingPathsDoNotResolveUnrelatedBasenames() throws Exception {
        assertNull(docs.getDocContent("missing/README.md"));
        assertNull(docs.getDocContent("../README.md"));
        assertNull(docs.getDocContent("/README.md"));
        assertNull(docs.getDocContent("core/*.md"));
        mvc.perform(get("/docs/missing/README.md")).andExpect(status().isNotFound());
    }

    @Test void relativeLinksAndHeadingIdsSurviveRendering() {
        String html = docs.renderMarkdown("tutorials/first-page", """
            # Example
            ## Details
            ## Details
            [Reference](../reference/rendering.md#slots)
            [Top](#example)
            <script>alert(1)</script>
            [unsafe](javascript:alert(1))
            """);
        var doc = Jsoup.parse(html);
        assertNotNull(doc.getElementById("details-1"));
        assertEquals("/docs/reference/rendering.md#slots", doc.select("a").get(0).attr("href"));
        assertEquals("/docs/tutorials/first-page.md#example", doc.select("a").get(1).attr("href"));
        assertTrue(doc.select("script").isEmpty());
        assertFalse(doc.select("a").get(2).attr("href").startsWith("javascript:"));
    }
}
