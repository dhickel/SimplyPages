package io.mindspice.demo;

import io.mindspice.simplypages.components.Div;
import io.mindspice.simplypages.components.Header;
import io.mindspice.simplypages.components.navigation.Link;
import io.mindspice.simplypages.core.Component;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DocumentationService {

    public static final String DEFAULT_DOC_PATH = "getting-started/01-installation-and-first-static-page.md";
    private static final String CLASSPATH_DOCS_ROOT = "static/docs/";
    private static final String CLASSPATH_DOCS_SCAN_PATTERN = "classpath*:static/docs/**/*.md";
    private static final String CLASSPATH_DOCS_FILE_PATTERN = "classpath*:static/docs/**/";
    private static final List<Path> FILESYSTEM_DOCS_ROOTS = List.of(Path.of("docs"), Path.of("../docs"));
    private static final String MISSING_NAV_MESSAGE = "Documentation navigation unavailable: no markdown files were discovered under static/docs.";

    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private final Map<String, String> docsCache = new ConcurrentHashMap<>();
    private volatile Map<String, List<String>> cachedDocsStructure;

    public String normalizePath(String requestUri) {
        String path = "";
        if (requestUri.length() > "/docs/".length()) {
            path = requestUri.substring("/docs/".length());
        }

        if (path.isEmpty() || "/".equals(path)) {
            path = DEFAULT_DOC_PATH;
        }

        if (path.equals("getting-started/01-introduction") || path.equals("getting-started/01-introduction.md")) {
            path = DEFAULT_DOC_PATH;
        }

        return path;
    }

    public String getDocContent(String path) {
        return docsCache.computeIfAbsent(path, this::loadDocContent);
    }

    public Component getDocsNavigation() {
        Map<String, List<String>> structure = getDocsStructure();
        Div navContainer = new Div().withClass("docs-nav");

        if (structure.isEmpty()) {
            return navContainer.withChild(
                new Div()
                    .withClass("alert alert-warning")
                    .withInnerText(MISSING_NAV_MESSAGE)
            );
        }

        java.util.function.BiConsumer<Div, String> addLink = (container, filePath) -> {
            String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf('/') + 1) : filePath;
            String title = fileName.replace(".md", "").replace("-", " ");
            if (title.matches("^\\d+\\s.*")) {
                title = title.replaceAll("^\\d+\\s", "");
            }
            title = title.substring(0, 1).toUpperCase() + title.substring(1);

            container.withChild(new Div().withClass("mb-1").withChild(
                Link.create("/docs/" + filePath, title)
                    .withHxGet("/docs/" + filePath)
                    .withHxTarget("#docs-content")
                    .withHxSwap("innerHTML show:window:top")
                    .withHxPushUrl(true)
                    .withClass("text-decoration-none text-dark")
            ));
        };

        if (structure.containsKey("Getting Started")) {
            navContainer.withChild(Header.H4("Getting Started").withClass("mb-2 mt-3"));
            structure.get("Getting Started").stream().sorted().forEach(file -> addLink.accept(navContainer, file));
        }

        structure.forEach((section, files) -> {
            if (!"Getting Started".equals(section)) {
                navContainer.withChild(Header.H4(section).withClass("mb-2 mt-4"));
                files.stream().sorted().forEach(file -> addLink.accept(navContainer, file));
            }
        });

        return navContainer;
    }

    private String loadDocContent(String path) {
        if (path.contains("..")) {
            return null;
        }

        String lookupPath = path.endsWith(".md") ? path : path + ".md";

        try {
            Resource resource = resourceResolver.getResource("classpath:" + CLASSPATH_DOCS_ROOT + lookupPath);
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }

            String fileName = lookupPath.contains("/") ? lookupPath.substring(lookupPath.lastIndexOf('/') + 1) : lookupPath;
            Resource[] resources = resourceResolver.getResources(CLASSPATH_DOCS_FILE_PATTERN + fileName);
            if (resources.length > 0) {
                return new String(resources[0].getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }

        } catch (IOException e) {
            // fall through and try filesystem fallback
        }

        for (Path docsRoot : FILESYSTEM_DOCS_ROOTS) {
            Path normalizedRoot = docsRoot.normalize();
            Path fallbackPath = normalizedRoot.resolve(lookupPath).normalize();
            if (fallbackPath.startsWith(normalizedRoot) && Files.exists(fallbackPath)) {
                try {
                    return Files.readString(fallbackPath, StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private synchronized Map<String, List<String>> getDocsStructure() {
        if (cachedDocsStructure != null) {
            return cachedDocsStructure;
        }

        Map<String, List<String>> sections = new TreeMap<>();

        try {
            Resource[] resources = resourceResolver.getResources(CLASSPATH_DOCS_SCAN_PATTERN);
            addClasspathResources(resources, sections);
        } catch (IOException ignored) {
            // Continue with filesystem fallback.
        }

        if (sections.isEmpty()) {
            addFilesystemResources(sections);
        }

        cachedDocsStructure = sections;
        return cachedDocsStructure;
    }

    private void addClasspathResources(Resource[] resources, Map<String, List<String>> sections) {
        for (Resource resource : resources) {
            try {
                String path = extractDocsRelativePath(resource.getURL().toString());
                if (path != null) {
                    sections.computeIfAbsent(toSectionName(path), key -> new ArrayList<>()).add(path);
                }
            } catch (IOException ignored) {
                // Skip unreadable resource and continue.
            }
        }
    }

    private void addFilesystemResources(Map<String, List<String>> sections) {
        for (Path docsRoot : FILESYSTEM_DOCS_ROOTS) {
            Path normalizedRoot = docsRoot.normalize();
            if (!Files.isDirectory(normalizedRoot)) {
                continue;
            }

            try (var stream = Files.walk(normalizedRoot)) {
                stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".md"))
                    .forEach(path -> {
                        String relativePath = normalizedRoot.relativize(path).toString().replace('\\', '/');
                        sections.computeIfAbsent(toSectionName(relativePath), key -> new ArrayList<>()).add(relativePath);
                    });
                return;
            } catch (IOException ignored) {
                // Try the next root.
            }
        }
    }

    static String extractDocsRelativePath(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }

        String normalized = location.replace('\\', '/').replaceAll("/+", "/");
        int docsIndex = normalized.lastIndexOf(CLASSPATH_DOCS_ROOT);
        if (docsIndex < 0) {
            return null;
        }
        String path = normalized.substring(docsIndex + CLASSPATH_DOCS_ROOT.length());
        if (path.isBlank() || !path.endsWith(".md")) {
            return null;
        }
        return path;
    }

    private String toSectionName(String relativePath) {
        String folder = "General";
        if (relativePath.contains("/")) {
            folder = relativePath.substring(0, relativePath.lastIndexOf('/'));
            folder = Arrays.stream(folder.split("-"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(" "));
        }
        return folder;
    }
}
