package io.mindspice.simplypages.components.content;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Generated static content entry metadata and markdown source.
 */
public record ContentEntryRecord(
    String sectionKey,
    String slug,
    String title,
    String summary,
    LocalDateTime publishedAt,
    List<String> tags,
    String markdown,
    Path sourcePath,
    String route,
    Map<String, String> metadata
) {
    public ContentEntryRecord {
        sectionKey = Objects.requireNonNull(sectionKey, "sectionKey");
        slug = Objects.requireNonNull(slug, "slug");
        title = title == null ? "" : title;
        summary = summary == null ? "" : summary;
        tags = tags == null ? List.of() : List.copyOf(tags);
        markdown = markdown == null ? "" : markdown;
        sourcePath = sourcePath == null ? null : sourcePath.toAbsolutePath().normalize();
        route = Objects.requireNonNull(route, "route");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
