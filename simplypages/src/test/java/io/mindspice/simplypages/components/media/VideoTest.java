package io.mindspice.simplypages.components.media;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VideoTest {

    @Test
    @DisplayName("Video should render controls and poster")
    void testVideoRendering() {
        Video video = Video.create("/video.mp4")
            .withControls()
            .withPoster("/poster.png")
            .withWidth(640)
            .withHeight(360);

        String html = video.render();

        assertTrue(html.contains("controls"));
        assertTrue(html.contains("poster=\"/poster.png\""));
        assertTrue(html.contains("width=\"640\""));
        assertTrue(html.contains("height=\"360\""));
    }

    @Test
    @DisplayName("Video should render playback attributes and preload")
    void testVideoPlaybackAttributes() {
        Video video = Video.create("/video.mp4")
            .withControls(false)
            .withAutoplay()
            .withLoop()
            .withMuted()
            .withPreload("metadata")
            .withWidth("100%")
            .withHeight("480");

        String html = video.render();

        assertTrue(html.contains("autoplay"));
        assertTrue(html.contains("loop"));
        assertTrue(html.contains("muted"));
        assertTrue(html.contains("preload=\"metadata\""));
        assertTrue(html.contains("width=\"100%\""));
        assertTrue(html.contains("height=\"480\""));
        assertFalse(html.contains("controls"));
    }
}
