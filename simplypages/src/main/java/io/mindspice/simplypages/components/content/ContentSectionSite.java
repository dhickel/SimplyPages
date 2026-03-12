package io.mindspice.simplypages.components.content;

import io.mindspice.simplypages.core.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generated artifacts for one configured static content section.
 */
public record ContentSectionSite(
    String sectionKey,
    String basePath,
    Map<Integer, Component> indexPagesByPage,
    Map<String, Component> detailPagesBySlug,
    List<ContentEntryRecord> entries
) {
    public ContentSectionSite {
        sectionKey = Objects.requireNonNull(sectionKey, "sectionKey");
        basePath = Objects.requireNonNull(basePath, "basePath");
        indexPagesByPage = indexPagesByPage == null ? Map.of() : Map.copyOf(indexPagesByPage);
        detailPagesBySlug = detailPagesBySlug == null ? Map.of() : Map.copyOf(detailPagesBySlug);
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public int totalPages() {
        return indexPagesByPage.isEmpty() ? 1 : indexPagesByPage.size();
    }
}
