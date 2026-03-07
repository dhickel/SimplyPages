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
            Resource resource = resourceResolver.getResource("classpath:static/docs/" + lookupPath);
            if (resource.exists()) {
                return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }

            String fileName = lookupPath.contains("/") ? lookupPath.substring(lookupPath.lastIndexOf('/') + 1) : lookupPath;
            Resource[] resources = resourceResolver.getResources("classpath:static/docs/**/" + fileName);
            if (resources.length > 0) {
                return new String(resources[0].getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            }

        } catch (IOException e) {
            return null;
        }

        return null;
    }

    private synchronized Map<String, List<String>> getDocsStructure() {
        if (cachedDocsStructure != null) {
            return cachedDocsStructure;
        }

        try {
            Resource[] resources = resourceResolver.getResources("classpath:static/docs/**/*.md");
            Map<String, List<String>> sections = new TreeMap<>();

            for (Resource resource : resources) {
                String relativePath = resource.getURL().toString();
                if (relativePath.contains("/docs/")) {
                    String path = relativePath.substring(relativePath.indexOf("/docs/") + 6);
                    String folder = "General";
                    if (path.contains("/")) {
                        folder = path.substring(0, path.lastIndexOf('/'));
                        folder = Arrays.stream(folder.split("-"))
                            .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                            .collect(Collectors.joining(" "));
                    }
                    sections.computeIfAbsent(folder, key -> new ArrayList<>()).add(path);
                }
            }

            cachedDocsStructure = sections;
            return cachedDocsStructure;
        } catch (IOException e) {
            return new TreeMap<>();
        }
    }
}
