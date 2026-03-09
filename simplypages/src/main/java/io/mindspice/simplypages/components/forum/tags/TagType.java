package io.mindspice.simplypages.components.forum.tags;

import java.util.Objects;

/**
 * Normalized forum tag type key.
 */
public record TagType(String key) {
    public TagType {
        key = ForumTagParser.normalizeKey(Objects.requireNonNull(key, "key"));
    }

    public static TagType of(String rawKey) {
        return new TagType(rawKey);
    }
}
