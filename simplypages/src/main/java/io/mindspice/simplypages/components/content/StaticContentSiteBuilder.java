package io.mindspice.simplypages.components.content;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builder for immutable {@link StaticContentSite} generation configuration.
 */
public final class StaticContentSiteBuilder {

    private final Map<String, ContentSectionConfig> sections = new LinkedHashMap<>();

    private StaticContentSiteBuilder() {}

    public static StaticContentSiteBuilder create() {
        return new StaticContentSiteBuilder();
    }

    public StaticContentSiteBuilder addSection(ContentSectionConfig sectionConfig) {
        ContentSectionConfig config = Objects.requireNonNull(sectionConfig, "sectionConfig");
        String sectionKey = config.sectionKey();
        if (sections.containsKey(sectionKey)) {
            throw new IllegalArgumentException("Duplicate sectionKey: " + sectionKey);
        }

        for (ContentSectionConfig existing : sections.values()) {
            if (existing.basePath().equals(config.basePath())) {
                throw new IllegalArgumentException("Duplicate basePath: " + config.basePath());
            }
        }

        sections.put(sectionKey, config);
        return this;
    }

    public StaticContentSite build() {
        return new StaticContentSite(sections.values());
    }
}
