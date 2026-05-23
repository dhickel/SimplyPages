package io.mindspice.simplypages.components.chat;

import java.util.Collection;
import java.util.List;

/**
 * Data contract for timeline transcript entries.
 */
public interface TranscriptEntryData {
    String id();
    String title();
    String body();

    default String actor() {
        return "";
    }

    default String timestamp() {
        return "";
    }

    default String status() {
        return "";
    }

    default Collection<? extends EmbeddedBlockData> embeddedBlocks() {
        return List.of();
    }
}
