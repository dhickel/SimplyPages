package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroModuleTest {

    @Test
    @DisplayName("HeroModule should render background styles and centered content")
    void testHeroCenteredWithBackground() {
        HeroModule module = HeroModule.create()
            .withTitle("Welcome")
            .withSubtitle("Subtitle")
            .withDescription("Description")
            .withBackgroundImage("/bg.png")
            .withBackgroundColor("#fff")
            .withPrimaryButton("Start", null)
            .withSecondaryButton("Learn", "/learn")
            .centered()
            .withCustomContent(new Paragraph("Custom"));

        String html = module.render();

        assertTrue(html.contains("background-image"));
        assertTrue(html.contains("background-color: #fff"));
        assertTrue(html.contains("hero-content align-center"));
        assertTrue(html.contains("align-center"));
        assertTrue(html.contains("btn btn-primary"));
        assertTrue(html.contains("href=\"#\""));
        assertTrue(html.contains("btn btn-secondary"));
        assertTrue(html.contains("href=\"/learn\""));
        assertTrue(html.contains("Custom"));
    }
}
