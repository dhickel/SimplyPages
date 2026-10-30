package io.mindspice.demo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationServiceTest {

    @Test
    @DisplayName("extractDocsRelativePath should normalize jar and file resource URLs")
    void extractDocsRelativePathShouldHandleCommonResourceSchemes() {
        assertEquals(
            "core/01-components-htmltag-and-module-lifecycle.md",
            DocumentationService.extractDocsRelativePath(
                "jar:nested:/opt/apps/simplypages-site.jar/!BOOT-INF/classes/!/static/docs/core/01-components-htmltag-and-module-lifecycle.md"
            )
        );
        assertEquals(
            "reference/components-and-modules-catalog.md",
            DocumentationService.extractDocsRelativePath(
                "file:/home/site/target/classes/static/docs/reference/components-and-modules-catalog.md"
            )
        );
        assertEquals(
            "operations/02-testing-and-troubleshooting-playbook.md",
            DocumentationService.extractDocsRelativePath(
                "file:C:\\\\apps\\\\demo\\\\target\\\\classes\\\\static\\\\docs\\\\operations\\\\02-testing-and-troubleshooting-playbook.md"
            )
        );
    }

    @Test
    @DisplayName("docs navigation should render discovered markdown links")
    void docsNavigationShouldIncludeLinks() {
        DocumentationService service = new DocumentationService();
        String rendered = service.getDocsNavigation().render();
        assertTrue(rendered.contains("/docs/"), "Expected docs navigation to include at least one docs link");
        assertTrue(rendered.contains("data-sp-scroll-top=\"target\""), "Expected docs HTMX links to tag target-top scroll reset");
    }

    @Test
    void filesystemOnlyDocumentsAreIgnoredOutsideCheckout(@TempDir Path temporary) throws Exception {
        Path working = Files.createDirectories(temporary.resolve("working"));
        for (Path root : new Path[] {working.resolve("docs"), temporary.resolve("docs")}) {
            Files.createDirectories(root);
            Files.writeString(root.resolve("filesystem-only.md"), "# Filesystem poison\nNever serve this.");
        }
        String classpath = Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
            .map(entry -> Path.of(entry).toAbsolutePath().toString())
            .collect(Collectors.joining(File.pathSeparator));
        Path output = temporary.resolve("probe.log");
        Process probe = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
            "-cp", classpath, ResourceOnlyProbe.class.getName())
            .directory(working.toFile()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(probe.waitFor(30, TimeUnit.SECONDS), "Resource-only probe timed out");
            assertEquals(0, probe.exitValue(), Files.readString(output));
        } finally {
            probe.destroyForcibly();
        }
    }

    public static class ResourceOnlyProbe {
        public static void main(String[] args) throws Exception {
            DocumentationService packaged = new DocumentationService();
            if (packaged.getDocContent("README.md") == null
                || packaged.getDocContent("filesystem-only.md") != null
                || packaged.getDocsNavigation().render().contains("filesystem-only")) {
                throw new AssertionError("Only packaged documents may load or appear in navigation");
            }
            ClassLoader original = Thread.currentThread().getContextClassLoader();
            try (var empty = new URLClassLoader(new java.net.URL[0], null)) {
                Thread.currentThread().setContextClassLoader(empty);
                DocumentationService unavailable = new DocumentationService();
                if (unavailable.getDocContent("filesystem-only.md") != null
                    || !unavailable.getDocsNavigation().render().contains("Documentation navigation unavailable")) {
                    throw new AssertionError("Missing resources must not activate filesystem navigation or content");
                }
            } finally {
                Thread.currentThread().setContextClassLoader(original);
            }
        }
    }

    @Test
    @DisplayName("default docs page content should resolve from packaged resources")
    void defaultDocsContentShouldResolve() {
        DocumentationService service = new DocumentationService();
        String content = service.getDocContent(DocumentationService.DEFAULT_DOC_PATH);
        assertNotNull(content, "Expected default docs content to be discoverable");
        assertTrue(content.contains("SimplyPages") || content.contains("Installation"), "Expected markdown content");
    }
}
