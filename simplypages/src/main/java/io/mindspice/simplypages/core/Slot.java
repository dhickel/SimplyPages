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
        Optional<T> valueOpt = context.get(key);
        if (valueOpt.isEmpty()) {
            return "";
        }

        Object value = valueOpt.get();
        if (value instanceof Component) {
            return ((Component) value).render(context);
        } else if (value != null) {
            // For strings/other types, we assume they need escaping unless specific slot types dictate otherwise.
            // But since Slot<T> is generic, we default to toString() + escape.
            return Encode.forHtml(value.toString());
        }
        return "";
    }
}
