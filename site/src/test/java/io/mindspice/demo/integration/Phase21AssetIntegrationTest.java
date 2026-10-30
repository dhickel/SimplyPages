package io.mindspice.demo.integration;

import io.mindspice.demo.DemoApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.http.MediaType;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

/** Real embedded Boot HTTP serving, including Spring's versionless lite WebJar resolver. */
@SpringBootTest(classes = DemoApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Phase21AssetIntegrationTest {
    @Autowired private ServletWebServerApplicationContext context;

    @Test
    void versionlessProductionMinifiedHtmxMatchesUnmodifiedClasspathWebjar() throws Exception {
        byte[] bytes = assertAsset("/webjars/htmx.org/dist/htmx.min.js",
            "META-INF/resources/webjars/htmx.org/2.0.11/dist/htmx.min.js");
        assertEquals(52182, bytes.length);
        assertEquals("d6fdc75f204e6bdefa99b69bf1e6d4ac69b8a364f77929f45c13476b4000f717",
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        assertNotNull(Class.forName("org.webjars.WebJarVersionLocator"));
        assertNotNull(Class.forName("org.springframework.web.servlet.resource.LiteWebJarsResourceResolver"));
    }

    @Test
    void frameworkJavascriptUsesActualClasspathBytes() throws Exception {
        assertAsset("/js/framework.js", "static/js/framework.js");
    }

    private byte[] assertAsset(String path, String resource) throws Exception {
        byte[] expected;
        try (var input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input, resource);
            expected = input.readAllBytes();
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + context.getWebServer().getPort() + path))
                .GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode(), path);
            var type = MediaType.parseMediaType(response.headers().firstValue("Content-Type").orElseThrow());
            assertTrue(type.isCompatibleWith(MediaType.parseMediaType("text/javascript"))
                || type.isCompatibleWith(MediaType.parseMediaType("application/javascript")), type.toString());
            assertArrayEquals(expected, response.body(), path);
            return response.body();
        }
    }
}
