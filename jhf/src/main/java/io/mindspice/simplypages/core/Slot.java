package io.mindspice.simplypages.core;

import org.owasp.encoder.Encode;

import java.util.Optional;

/**
 * A component that renders a value from the {@link RenderContext}.
 *
 * @param <T> the type of the value
 */
public class Slot<T> implements Component {
    private final SlotKey<T> key;

    private Slot(SlotKey<T> key) {
        this.key = key;
    }

    public static <T> Slot<T> of(SlotKey<T> key) {
        return new Slot<>(key);
    }

    public SlotKey<T> getKey() {
        return key;
    }

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
