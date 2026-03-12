package io.mindspice.simplypages.components.content;

import io.mindspice.simplypages.core.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Route mapping and lookup helper for generated static content pages.
 */
public final class ContentRouteIndex {

    private static final String PAGE_QUERY_KEY = "page";

    private final Map<String, String> sectionBasePaths;
    private final Map<String, Map<Integer, Component>> indexBySectionPage;
    private final Map<String, Map<String, Component>> detailBySectionSlug;
    private final Map<String, Component> indexPagesByRoute;
    private final Map<String, Component> detailPagesByRoute;
    private final Map<String, String> sectionByBasePath;

    ContentRouteIndex(
        Map<String, String> sectionBasePaths,
        Map<String, Map<Integer, Component>> indexBySectionPage,
        Map<String, Map<String, Component>> detailBySectionSlug
    ) {
        this.sectionBasePaths = Map.copyOf(sectionBasePaths);
        this.indexBySectionPage = deepCopy(indexBySectionPage);
        this.detailBySectionSlug = deepCopy(detailBySectionSlug);

        Map<String, Component> indexRoutes = new LinkedHashMap<>();
        Map<String, Component> detailRoutes = new LinkedHashMap<>();
        Map<String, String> basePathToSection = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : this.sectionBasePaths.entrySet()) {
            String sectionKey = entry.getKey();
            String basePath = entry.getValue();
            basePathToSection.put(basePath, sectionKey);

            Map<Integer, Component> pageMap = this.indexBySectionPage.getOrDefault(sectionKey, Map.of());
            for (Map.Entry<Integer, Component> pageEntry : pageMap.entrySet()) {
                String route = indexRoute(sectionKey, pageEntry.getKey());
                indexRoutes.put(route, pageEntry.getValue());
            }

            Map<String, Component> detailMap = this.detailBySectionSlug.getOrDefault(sectionKey, Map.of());
            for (Map.Entry<String, Component> detailEntry : detailMap.entrySet()) {
                String route = detailRoute(sectionKey, detailEntry.getKey());
                detailRoutes.put(route, detailEntry.getValue());
            }
        }

        this.indexPagesByRoute = Map.copyOf(indexRoutes);
        this.detailPagesByRoute = Map.copyOf(detailRoutes);
        this.sectionByBasePath = Map.copyOf(basePathToSection);
    }

    public Set<String> sectionKeys() {
        return sectionBasePaths.keySet();
    }

    public String indexRoute(String sectionKey, int page) {
        String basePath = sectionBasePath(sectionKey);
        if (page <= 1) {
            return basePath;
        }
        return basePath + "?" + PAGE_QUERY_KEY + "=" + page;
    }

    public String detailRoute(String sectionKey, String slug) {
        String basePath = sectionBasePath(sectionKey);
        if (slug == null || slug.isBlank()) {
            throw new IllegalArgumentException("slug cannot be null or blank");
        }
        return basePath + "/" + slug;
    }

    public Optional<Component> resolveIndex(String sectionKey, int page) {
        Map<Integer, Component> pages = indexBySectionPage.get(sectionKey);
        if (pages == null || pages.isEmpty()) {
            return Optional.empty();
        }
        int safePage = page < 1 ? 1 : page;
        return Optional.ofNullable(pages.get(safePage));
    }

    public Optional<Component> resolveDetail(String sectionKey, String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        Map<String, Component> pages = detailBySectionSlug.get(sectionKey);
        if (pages == null || pages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(pages.get(slug));
    }

    /**
     * Resolves a request using path + page query value, where detail routes take precedence.
     */
    public Optional<Component> resolveRequest(String requestPath, String pageParamValue) {
        String normalizedPath = normalizePath(requestPath);

        Component detail = detailPagesByRoute.get(normalizedPath);
        if (detail != null) {
            return Optional.of(detail);
        }

        String sectionKey = sectionByBasePath.get(normalizedPath);
        if (sectionKey == null) {
            return Optional.empty();
        }

        return resolveIndex(sectionKey, parsePage(pageParamValue));
    }

    /**
     * Resolves a full route value (`/blogs?page=2`, `/blogs/slug`).
     */
    public Optional<Component> resolveRoute(String route) {
        if (route == null || route.isBlank()) {
            return Optional.empty();
        }

        String value = route.trim();
        int queryIndex = value.indexOf('?');
        if (queryIndex < 0) {
            return resolveRequest(value, null);
        }

        String path = value.substring(0, queryIndex);
        String query = value.substring(queryIndex + 1);
        return resolveRequest(path, queryParam(query, PAGE_QUERY_KEY));
    }

    public Map<String, Component> indexPagesByRoute() {
        return indexPagesByRoute;
    }

    public Map<String, Component> detailPagesByRoute() {
        return detailPagesByRoute;
    }

    private String sectionBasePath(String sectionKey) {
        String path = sectionBasePaths.get(sectionKey);
        if (path == null) {
            throw new IllegalArgumentException("Unknown section key: " + sectionKey);
        }
        return path;
    }

    private static int parsePage(String pageValue) {
        if (pageValue == null || pageValue.isBlank()) {
            return 1;
        }
        try {
            int page = Integer.parseInt(pageValue);
            return Math.max(1, page);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static String queryParam(String query, String key) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String[] parts = query.split("&");
        for (String part : parts) {
            int equals = part.indexOf('=');
            if (equals < 0) {
                continue;
            }
            String paramKey = part.substring(0, equals).trim();
            if (key.equals(paramKey)) {
                return part.substring(equals + 1).trim();
            }
        }
        return null;
    }

    private static String normalizePath(String requestPath) {
        String value = requestPath == null ? "" : requestPath.trim();
        if (value.isEmpty()) {
            return "/";
        }
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static <K1, K2, V> Map<K1, Map<K2, V>> deepCopy(Map<K1, Map<K2, V>> input) {
        Map<K1, Map<K2, V>> copy = new LinkedHashMap<>();
        for (Map.Entry<K1, Map<K2, V>> entry : input.entrySet()) {
            copy.put(entry.getKey(), Map.copyOf(entry.getValue()));
        }
        return Map.copyOf(copy);
    }
}
