package io.mindspice.simplypages.components.forum.tags;

import java.util.Objects;

/**
 * Forum tag token value coupled to its type.
 */
public record Tag(TagType type, String value) {
    public Tag {
        type = Objects.requireNonNull(type, "type");
        value = value == null ? "" : value;
    }
}
