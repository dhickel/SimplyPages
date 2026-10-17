package io.mindspice.simplypages.modules;

import io.mindspice.simplypages.components.Paragraph;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    @DisplayName("HeroModule keeps caller-controlled button and background URLs")
    void testCallerControlledUrls() {
        var document = org.jsoup.Jsoup.parse(HeroModule.create()
                .withPrimaryButton("Bad", "javascript:alert(1)")
                .withBackgroundImage("/bg.png\");color:red").render());
        org.junit.jupiter.api.Assertions.assertEquals("javascript:alert(1)", document.selectFirst("a").attr("href"));
        org.junit.jupiter.api.Assertions.assertEquals("background-image: url('/bg.png\");color:red'); "
                        + "background-size: cover; background-position: center;",
                document.selectFirst(".hero-module").attr("style"));
    }

    @Test
    @DisplayName("HeroModule background colors retain caller-owned CSS syntax")
    void testCallerControlledStyleValues() {
        HeroModule module = HeroModule.create().withBackgroundColor("#fff;color:red");

        org.junit.jupiter.api.Assertions.assertEquals("background-color: #fff;color:red;",
                org.jsoup.Jsoup.parse(module.render()).selectFirst(".hero-module").attr("style"));
    }
}
