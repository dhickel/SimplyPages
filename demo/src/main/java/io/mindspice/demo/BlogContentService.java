package io.mindspice.demo;

import io.mindspice.simplypages.components.content.ContentSectionConfig;
import io.mindspice.simplypages.components.content.ContentSiteBundle;
import io.mindspice.simplypages.components.content.ContentWarning;
import io.mindspice.simplypages.components.content.StaticContentSite;
import io.mindspice.simplypages.components.content.StaticContentSiteBuilder;
import io.mindspice.simplypages.core.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;

@Service
public class BlogContentService {

    private static final Logger LOG = LoggerFactory.getLogger(BlogContentService.class);
    private static final String BLOG_SECTION_KEY = "blog";
    private static final String BLOG_BASE_PATH = "/blog";
    private static final String CLASSPATH_BLOG_ROOT = "static/blog/";
    private static final String CLASSPATH_BLOG_SCAN_PATTERN = "classpath*:static/blog/**/*.md";

    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private final ContentSiteBundle bundle;

    public BlogContentService() {
        this.bundle = buildBundle();
    }

    public Optional<Component> resolveRequest(String requestPath, String pageParam) {
        return bundle.routeIndex().resolveRequest(requestPath, pageParam);
    }

    private ContentSiteBundle buildBundle() {
        Path blogDirectory = resolveBlogDirectory();
        StaticContentSite site = StaticContentSiteBuilder.create()
            .addSection(ContentSectionConfig.create(BLOG_SECTION_KEY, blogDirectory, BLOG_BASE_PATH)
                .withSectionTitle("SimplyPages Blog")
                .withPageSize(5))
            .build();

        ContentSiteBundle generated = site.generate();
        for (ContentWarning warning : generated.warnings()) {
            LOG.warn(
                "Blog content warning [section={}, path={}]: {}",
                warning.sectionKey(),
                warning.sourcePath(),
                warning.message()
            );
        }
        return generated;
    }

    private Path resolveBlogDirectory() {
        List<Path> candidates = List.of(
            Path.of("src/main/resources/static/blog"),
            Path.of("demo/src/main/resources/static/blog"),
            Path.of("target/classes/static/blog")
        );

        for (Path candidate : candidates) {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (Files.isDirectory(normalized)) {
                return normalized;
            }
        }

        Resource classpathBlog = resourceResolver.getResource("classpath:static/blog");
        if (classpathBlog.exists()) {
            try {
                Path classpathPath = classpathBlog.getFile().toPath().toAbsolutePath().normalize();
                if (Files.isDirectory(classpathPath)) {
                    return classpathPath;
                }
            } catch (IOException ignored) {
                // Continue to classpath materialization path.
            }
        }

        Path materialized = materializeClasspathBlogDirectory();
        if (materialized != null) {
            return materialized;
        }

        return candidates.getFirst().toAbsolutePath().normalize();
    }

    private Path materializeClasspathBlogDirectory() {
        try {
            Resource[] resources = resourceResolver.getResources(CLASSPATH_BLOG_SCAN_PATTERN);
            if (resources.length == 0) {
                return null;
            }

            Path tempRoot = Files.createTempDirectory("simplypages-blog-content");
            tempRoot.toFile().deleteOnExit();

            for (Resource resource : resources) {
                String relativePath = extractBlogRelativePath(resource);
                if (relativePath == null) {
                    continue;
                }

                Path targetPath = tempRoot.resolve(relativePath).normalize();
                if (!targetPath.startsWith(tempRoot)) {
                    continue;
                }

                Path parent = targetPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                try (InputStream inputStream = resource.getInputStream()) {
                    Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            return tempRoot;
        } catch (IOException e) {
            LOG.warn("Failed to materialize classpath blog content: {}", e.getMessage());
            return null;
        }
    }

    private String extractBlogRelativePath(Resource resource) {
        try {
            String location = resource.getURL().toString().replace('\\', '/');
            int rootIndex = location.lastIndexOf(CLASSPATH_BLOG_ROOT);
            if (rootIndex < 0) {
                return null;
            }

            String relativePath = location.substring(rootIndex + CLASSPATH_BLOG_ROOT.length());
            int queryIndex = relativePath.indexOf('?');
            if (queryIndex >= 0) {
                relativePath = relativePath.substring(0, queryIndex);
            }
            if (relativePath.isBlank()) {
                return null;
            }
            return relativePath;
        } catch (IOException ignored) {
            return null;
        }
    }
}
