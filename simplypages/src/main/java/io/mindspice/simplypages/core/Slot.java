package io.mindspice.simplypages.core;

import org.owasp.encoder.Encode;

import java.util.Optional;

/**
 * Component placeholder that renders the value for a {@link SlotKey} from a {@link RenderContext}.
 *
 * <p>Rendering contract:</p>
 * <p>- missing value: renders empty string</p>
 * <p>- value is {@link Component}: delegates to that component's render path</p>
 * <p>- all other values: HTML-escapes {@code toString()}</p>
 * <p>- compiled entries are trusted HTML; explicit live values may be cached by context policy</p>
 * <p>- applies equally for direct component rendering and {@link Template} rendering</p>
 *
 * <p>Mutability/thread-safety: immutable after construction and thread-safe.</p>
 *
 * @param <T> slot value type
 */
public class Slot<T> implements Component {
    private final SlotKey<T> key;

    /**
     * Creates a slot bound to {@code key}.
     */
    private Slot(SlotKey<T> key) {
        this.key = key;
    }

    /**
     * Factory for a slot bound to {@code key}.
     */
    public static <T> Slot<T> of(SlotKey<T> key) {
        return new Slot<>(key);
    }

    /**
     * Returns the key resolved at render time.
     */
    public SlotKey<T> getKey() {
        return key;
    }

    /**
     * Renders the current slot value using the supplied context.
     */
    @Override
    public String render(RenderContext context) {
        Optional<String> compiled = context.getCompiled(key);
        if (compiled.isPresent()) {
            return compiled.get();
        }
        Optional<T> valueOpt = context.get(key);
        if (valueOpt.isEmpty()) {
            return "";
        }
        Object value = valueOpt.get();
        String rendered = value instanceof Component component
            ? component.render(context) : Encode.forHtml(value.toString());
        if (context.getPolicy() == RenderContext.RenderPolicy.COMPILE_ON_FIRST_HIT
                && context.getEntry(key).isPresent()) {
            context.putCompiled(key, rendered);
        }
        return rendered;
    }
}
