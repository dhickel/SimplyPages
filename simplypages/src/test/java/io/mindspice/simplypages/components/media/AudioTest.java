package io.mindspice.simplypages.components.media;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AudioTest {

    @Test
    @DisplayName("Audio should render controls and preload")
    void testAudioRendering() {
        Audio audio = Audio.create("/audio.mp3")
            .withControls()
            .withPreload("auto");

        String html = audio.render();

        assertTrue(html.contains("controls"));
        assertTrue(html.contains("preload=\"auto\""));
    }

    @Test
    @DisplayName("Audio should render autoplay, loop, and muted attributes")
    void testAudioPlaybackAttributes() {
        Audio audio = Audio.create("/audio.mp3")
            .withControls(false)
            .withAutoplay()
            .withLoop()
            .withMuted();

        String html = audio.render();

        assertTrue(html.contains("autoplay"));
        assertTrue(html.contains("loop"));
        assertTrue(html.contains("muted"));
        assertFalse(html.contains("controls"));
    }
}
