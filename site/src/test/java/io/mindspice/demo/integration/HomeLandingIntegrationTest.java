package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class HomeLandingIntegrationTest {
    @Autowired MockMvc mvc;
    @Test void homeAndRootRenderTheOfficialLandingAndWorkingPreview() throws Exception {
        for (String route : new String[]{"/", "/home"}) {
            var doc = Jsoup.parse(mvc.perform(get(route)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertEquals(1, doc.select("main").size());
            assertEquals(1, doc.select("main h1").size());
            assertNotNull(doc.selectFirst(".sp-site-home .sp-site-preview .content-module"));
            assertTrue(doc.select("code").text().contains("ContentModule.create()"));
            for (String link : new String[]{"/", "/demos", "/api", "/docs", "/demos/features/forum", "/demos/features/chat", "/demos/features/editing"})
                assertNotNull(doc.selectFirst("a[href='" + link + "']"), link);
            assertFalse(doc.text().contains("Smoke Test"));
            assertTrue(doc.select("iframe").isEmpty());
        }
    }
}
