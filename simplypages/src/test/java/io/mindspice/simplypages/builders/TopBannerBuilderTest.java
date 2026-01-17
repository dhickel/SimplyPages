package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.core.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopBannerBuilderTest {

    @Test
    @DisplayName("TopBannerBuilder should render image, text, and styles")
    void testTopBannerWithImageAndStyles() {
        Component banner = TopBannerBuilder.create()
            .withTitle("My App")
            .withSubtitle("Subtitle")
            .withImage("/logo.png", "Logo")
            .withImageSize(120, 60)
            .withBackgroundColor("#111111")
            .withTextColor("#eeeeee")
            .withClass("custom-banner")
            .build();

        String html = banner.render();

        assertTrue(html.contains("top-banner custom-banner"));
        assertTrue(html.contains("background-color: #111111;"));
        assertTrue(html.contains("color: #eeeeee;"));
        assertTrue(html.contains("top-banner-image"));
        assertTrue(html.contains("width=\"120\""));
        assertTrue(html.contains("height=\"60\""));
        assertTrue(html.contains("top-banner-title"));
        assertTrue(html.contains("top-banner-subtitle"));
        assertTrue(html.contains("My App"));
        assertTrue(html.contains("Subtitle"));
    }

    @Test
    @DisplayName("TopBannerBuilder should allow custom content overrides")
    void testTopBannerWithCustomContent() {
        Component banner = TopBannerBuilder.create()
            .withTitle("Ignored")
            .withSubtitle("Ignored")
            .withCustomContent(new Div().withInnerText("Custom Content"))
            .build();

        String html = banner.render();

        assertTrue(html.contains("Custom Content"));
        assertFalse(html.contains("top-banner-title"));
    }
}
