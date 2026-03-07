package io.mindspice.simplypages.core;

/**
 * Tagged union for explicit slot storage entries inside {@link RenderContext}.
 *
 * <p>Mutability/thread-safety: all implementations are records and therefore immutable.</p>
 */
public sealed interface SlotEntry permits SlotEntry.LiveEntry, SlotEntry.CompiledEntry {

    /**
     * Entry containing a live object that must be rendered on access.
     *
     * @param type runtime class of {@link #value()}
     * @param value live value to render
     */
    record LiveEntry(Class<?> type, Object value) implements SlotEntry { }

    /**
     * Entry containing trusted pre-rendered HTML.
     *
     * @param html HTML fragment inserted without additional escaping
     */
    record CompiledEntry(String html) implements SlotEntry { }
}
