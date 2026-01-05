package io.mindspice.simplypages.components.navigation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkTest {

    @Test
    @DisplayName("Link should render href and HTMX attributes")
    void testLinkRendering() {
        Link link = Link.create("/path", "Go")
            .withHxGet("/data")
            .withHxTarget("#target")
            .withHxSwap("outerHTML");

        String html = link.render();

        assertTrue(html.contains("href=\"/path\""));
        assertTrue(html.contains("hx-get=\"/data\""));
        assertTrue(html.contains("hx-target=\"#target\""));
        assertTrue(html.contains("hx-swap=\"outerHTML\""));
    }

    @Test
    @DisplayName("Link should render target, id, and push-url attributes")
    void testLinkTargetAndId() {
        Link link = Link.create("/path", "Go")
            .withId("link-1")
            .openInNewTab()
            .withHxPost("/submit")
            .withHxPushUrl(true);

        String html = link.render();

        assertTrue(html.contains("id=\"link-1\""));
        assertTrue(html.contains("target=\"_blank\""));
        assertTrue(html.contains("hx-post=\"/submit\""));
        assertTrue(html.contains("hx-push-url=\"true\""));
    }

    @Test
    @DisplayName("Link should update href when set explicitly")
    void testLinkWithHref() {
        Link link = Link.create("/path", "Go")
            .withHref("https://example.com");

        String html = link.render();

        assertTrue(html.contains("href=\"https://example.com\""));
        assertFalse(html.contains("href=\"/path\""));
    }
}
