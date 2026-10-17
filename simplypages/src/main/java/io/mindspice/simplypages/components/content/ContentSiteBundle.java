package io.mindspice.simplypages.components.content;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Final static content bundle generated from configured section directories.
 * Inspect {@link #warnings()} after generation for nonfatal parsing and generation
 * diagnostics, including detail routes that shadow another section's index.
 * The warning list is immutable; warnings do not remove or reassign generated pages.
 */
public record ContentSiteBundle(
    Map<String, ContentSectionSite> sectionsByKey,
    ContentRouteIndex routeIndex,
    List<ContentWarning> warnings
) {
    public ContentSiteBundle {
        sectionsByKey = sectionsByKey == null ? Map.of() : Map.copyOf(sectionsByKey);
        routeIndex = Objects.requireNonNull(routeIndex, "routeIndex");
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
