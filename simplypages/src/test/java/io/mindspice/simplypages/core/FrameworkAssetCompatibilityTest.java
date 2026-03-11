package io.mindspice.simplypages.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameworkAssetCompatibilityTest {

    @Test
    @DisplayName("framework.css should keep legacy utility and banner aliases")
    void testFrameworkCssLegacyAliasesPresent() throws IOException {
        String css = readClasspathResource("static/css/framework.css");

        assertTrue(css.contains(".p-sm { padding: 10px; }"));
        assertTrue(css.contains(".p-medium { padding: 20px; }"));
        assertTrue(css.contains(".p-lg { padding: 30px; }"));
        assertTrue(css.contains(".m-sm { margin: 10px; }"));
        assertTrue(css.contains(".m-medium { margin: 20px; }"));
        assertTrue(css.contains(".m-lg { margin: 30px; }"));

        assertTrue(css.contains(".top-banner"));
        assertTrue(css.contains(".top-banner-content"));
        assertTrue(css.contains(".top-banner-image"));
        assertTrue(css.contains(".top-banner-text"));
        assertTrue(css.contains(".top-banner-title"));
        assertTrue(css.contains(".top-banner-subtitle"));
        assertTrue(css.contains(".banner-horizontal .banner-content:not(:has(.banner-image)):not(:has(.top-banner-image))"));
        assertTrue(css.contains(".banner-horizontal .banner-content > .banner-text:only-child"));
        assertTrue(css.contains(".banner-horizontal .banner-content,\n    .banner-horizontal .top-banner-content {\n        flex-direction: row;"));
        assertTrue(css.contains(".home-landing-nav-card"));
        assertTrue(css.contains(".mobile-sidebar-toggle"));
        assertTrue(css.contains(".sticky-sidebar-mobile-collapse"));
        assertTrue(css.contains(".sticky-sidebar-mobile-summary"));
    }

    @Test
    @DisplayName("framework.css should expose semantic theme tokens and keep hero styling contract")
    void testFrameworkCssThemeTokensAndHeroContract() throws IOException {
        String css = readClasspathResource("static/css/framework.css");

        assertTrue(css.contains("--sp-theme-bg-canvas"));
        assertTrue(css.contains("--sp-theme-accent"));
        assertTrue(css.contains("--sp-gradient-surface-panel"));
        assertTrue(css.contains("--sp-gradient-shell-bg"));
        assertTrue(css.contains("--sp-gradient-nav-shell"));
        assertTrue(css.contains("--sp-gradient-nav-shell-muted"));
        assertTrue(css.contains("--sp-gradient-banner"));
        assertTrue(css.contains("--sp-top-nav-bg"));
        assertTrue(css.contains("--sp-gradient-hero"));
        assertTrue(css.contains(".card {"));
        assertTrue(css.contains("background: var(--sp-gradient-surface-panel);"));
        assertTrue(css.contains(".banner,\n.top-banner {"));
        assertTrue(css.contains("background: var(--sp-gradient-banner);"));
        assertTrue(css.contains(".header-top-nav-wrap"));
        assertTrue(css.contains(".top-nav"));
        assertTrue(css.contains(".account-bar a {"));
        assertTrue(css.contains("background-color: var(--sp-top-nav-item-hover-bg);"));
        assertTrue(css.contains(".sidenav-item {"));
        assertTrue(css.contains("background: transparent;"));

        assertTrue(css.contains(".hero-module {"));
        assertTrue(css.contains("background: var(--sp-gradient-hero);"));
        assertTrue(css.contains(".hero-module .btn-primary:hover {"));
        assertTrue(css.contains("background-color: var(--sp-hero-button-primary-hover-bg);"));
    }

    @Test
    @DisplayName("framework.js should support tagged target-top reset and keep push-url fallback")
    void testFrameworkJsScrollGateLogic() throws IOException {
        String js = readClasspathResource("static/js/framework.js");

        assertTrue(js.contains("document.body.addEventListener('htmx:afterSettle'"));
        assertTrue(js.contains("data-sp-scroll-top"));
        assertTrue(js.contains("target.scrollIntoView({block: 'start', inline: 'nearest', behavior: 'auto'});"));
        assertTrue(js.contains("pushUrlRequest.trim().toLowerCase() !== 'false'"));
        assertTrue(js.contains("normalizedPushUrlAttr !== 'false'"));
        assertTrue(js.contains("window.scrollTo({top: 0, left: 0, behavior: 'auto'});"));
        assertTrue(js.contains("function toggleMobileSidebar()"));
        assertTrue(js.contains("sidebar.classList.toggle('mobile-open')"));
    }

    private String readClasspathResource(String path) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            assertNotNull(inputStream, "Missing classpath resource: " + path);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
