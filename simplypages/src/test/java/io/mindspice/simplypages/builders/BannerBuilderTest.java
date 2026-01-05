package io.mindspice.simplypages.builders;

import io.mindspice.simplypages.components.Paragraph;
import io.mindspice.simplypages.core.Component;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BannerBuilderTest {

    @Test
    @DisplayName("BannerBuilder should render horizontal layout with image and text")
    void testHorizontalBanner() {
        Component banner = BannerBuilder.create()
            .withLayout(BannerBuilder.BannerLayout.HORIZONTAL)
            .withImage("/logo.png", "Logo")
            .withImageSize(100, 50)
            .withTitle("Welcome")
            .withSubtitle("Subtitle")
            .withClass("promo")
            .build();

        String html = banner.render();

        assertTrue(html.contains("banner banner-horizontal promo"));
        assertTrue(html.contains("banner-image"));
        assertTrue(html.contains("width=\"100\""));
        assertTrue(html.contains("height=\"50\""));
        assertTrue(html.contains("banner-text"));
        assertTrue(html.contains("banner-title"));
        assertTrue(html.contains("banner-subtitle"));
    }

    @Test
    @DisplayName("BannerBuilder should render image overlay layout with alignment and styles")
    void testOverlayBanner() {
        Component banner = BannerBuilder.create()
            .withLayout(BannerBuilder.BannerLayout.IMAGE_OVERLAY)
            .withTextAlignment(BannerBuilder.TextAlignment.LEFT)
            .withBackgroundImage("/images/bg.jpg")
            .withMinHeight(240)
            .withTitle("Overlay")
            .withSubtitle("Details")
            .build();

        String html = banner.render();

        assertTrue(html.contains("banner banner-image-overlay"));
        assertTrue(html.contains("align-left"));
        assertTrue(html.contains("background-image: url"));
        assertTrue(html.contains("/images/bg.jpg"));
        assertTrue(html.contains("min-height: 240px;"));
        assertTrue(html.contains("Overlay"));
        assertTrue(html.contains("Details"));
    }

    @Test
    @DisplayName("BannerBuilder should render custom content and styles")
    void testCustomContentOverridesText() {
        Component banner = BannerBuilder.create()
            .withLayout(BannerBuilder.BannerLayout.CENTERED)
            .withTitle("Title")
            .withSubtitle("Subtitle")
            .withBackgroundColor("#fff")
            .withTextColor("#000")
            .withCustomContent(new Paragraph("Custom"))
            .build();

        String html = banner.render();

        assertTrue(html.contains("background-color: #fff"));
        assertTrue(html.contains("color: #000"));
        assertTrue(html.contains("Custom"));
        assertFalse(html.contains("banner-title"));
    }
}
