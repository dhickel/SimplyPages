package io.mindspice.simplypages.components.chat;

import io.mindspice.simplypages.core.Component;

/**
 * Generic embedded disclosure content for transcript-like timelines.
 */
public interface EmbeddedBlockData {
    String id();
    String label();

    default String kind() {
        return "";
    }

    default Component content() {
        return null;
    }

    default boolean open() {
        return false;
    }
}
