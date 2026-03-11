package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = DemoApplication.class)
@AutoConfigureMockMvc
class HomeLandingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Home page should render hero landing and visual nav cards")
    void homePageRendersHeroLanding() throws Exception {
        mockMvc.perform(get("/home"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("home-landing-stack")))
            .andExpect(content().string(containsString("hero-module")))
            .andExpect(content().string(containsString("home-landing-nav-grid")))
            .andExpect(content().string(containsString("home-landing-nav-card")))
            .andExpect(content().string(containsString("href=\"/home\"")))
            .andExpect(content().string(containsString("href=\"/demos\"")))
            .andExpect(content().string(containsString("href=\"/javadocs-view\"")))
            .andExpect(content().string(containsString("href=\"/forum\"")))
            .andExpect(content().string(containsString("href=\"/docs\"")));
    }

    @Test
    @DisplayName("Root route should resolve to the home landing page")
    void rootRouteRendersHomeLanding() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("home-landing-stack")))
            .andExpect(content().string(containsString("hero-module")))
            .andExpect(content().string(containsString("home-landing-nav-shell")));
    }
}
