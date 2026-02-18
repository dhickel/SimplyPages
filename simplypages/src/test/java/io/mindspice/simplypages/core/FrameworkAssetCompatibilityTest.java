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
    }

    @Test
    @DisplayName("framework.js should keep navigation-only scroll reset and handle false push-url")
    void testFrameworkJsScrollGateLogic() throws IOException {
        String js = readClasspathResource("static/js/framework.js");

        assertTrue(js.contains("document.body.addEventListener('htmx:afterSettle'"));
        assertTrue(js.contains("pushUrlRequest.trim().toLowerCase() !== 'false'"));
        assertTrue(js.contains("normalizedPushUrlAttr !== 'false'"));
        assertTrue(js.contains("window.scrollTo({top: 0, left: 0, behavior: 'auto'});"));
    }

    private String readClasspathResource(String path) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(path)) {
            assertNotNull(inputStream, "Missing classpath resource: " + path);
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
