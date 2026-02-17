package io.mindspice.simplypages.core;

/**
 * Represents an explicit slot entry stored in {@link RenderContext}.
 */
public sealed interface SlotEntry permits SlotEntry.LiveEntry, SlotEntry.CompiledEntry {

    /**
     * Live slot entry containing a value that should be rendered.
     */
    record LiveEntry(Class<?> type, Object value) implements SlotEntry { }

    /**
     * Compiled slot entry containing trusted, pre-rendered HTML.
     */
    record CompiledEntry(String html) implements SlotEntry { }
}
