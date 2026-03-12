package io.mindspice.simplypages.components.content;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Section configuration for {@link StaticContentSite} generation.
 */
public final class ContentSectionConfig {

    private final String sectionKey;
    private final Path directoryPath;
    private final String basePath;

    private String sectionTitle;
    private int pageSize = 10;
    private int maxDepth = 32;
    private boolean allowUnsafeMarkdown = false;
    private Supplier<? extends ContentListItemComponent> listItemComponentSupplier = DefaultContentListItemComponent::create;

    private ContentSectionConfig(String sectionKey, Path directoryPath, String basePath) {
        this.sectionKey = requireNonBlank(sectionKey, "sectionKey");
        this.directoryPath = Objects.requireNonNull(directoryPath, "directoryPath").toAbsolutePath().normalize();
        this.basePath = normalizeBasePath(basePath);
        this.sectionTitle = toDisplayLabel(sectionKey);
    }

    public static ContentSectionConfig create(String sectionKey, Path directoryPath, String basePath) {
        return new ContentSectionConfig(sectionKey, directoryPath, basePath);
    }

    public ContentSectionConfig withSectionTitle(String sectionTitle) {
        this.sectionTitle = requireNonBlank(sectionTitle, "sectionTitle");
        return this;
    }

    public ContentSectionConfig withPageSize(int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        this.pageSize = pageSize;
        return this;
    }

    public ContentSectionConfig withMaxDepth(int maxDepth) {
        if (maxDepth < 1) {
            throw new IllegalArgumentException("maxDepth must be >= 1");
        }
        this.maxDepth = maxDepth;
        return this;
    }

    public ContentSectionConfig withAllowUnsafeMarkdown(boolean allowUnsafeMarkdown) {
        this.allowUnsafeMarkdown = allowUnsafeMarkdown;
        return this;
    }

    public ContentSectionConfig allowUnsafeMarkdown() {
        this.allowUnsafeMarkdown = true;
        return this;
    }

    public ContentSectionConfig withListItemComponentSupplier(
        Supplier<? extends ContentListItemComponent> listItemComponentSupplier
    ) {
        this.listItemComponentSupplier = Objects.requireNonNull(listItemComponentSupplier, "listItemComponentSupplier");
        return this;
    }

    public String sectionKey() {
        return sectionKey;
    }

    public Path directoryPath() {
        return directoryPath;
    }

    public String basePath() {
        return basePath;
    }

    public String sectionTitle() {
        return sectionTitle;
    }

    public int pageSize() {
        return pageSize;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public boolean unsafeMarkdownEnabled() {
        return allowUnsafeMarkdown;
    }

    public Supplier<? extends ContentListItemComponent> listItemComponentSupplier() {
        return listItemComponentSupplier;
    }

    static String normalizeBasePath(String basePath) {
        String value = requireNonBlank(basePath, "basePath").trim();
        if (!value.startsWith("/")) {
            value = "/" + value;
        }
        if (value.length() > 1 && value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be null or blank");
        }
        return value;
    }

    private static String toDisplayLabel(String value) {
        String[] parts = value.replace('_', ' ').replace('-', ' ').trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            String lower = part.toLowerCase(Locale.ROOT);
            sb.append(Character.toUpperCase(lower.charAt(0))).append(lower.substring(1));
        }
        return sb.isEmpty() ? "Content" : sb.toString();
    }
}
